package com.mycompany.chat;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/chat")
public class ChatGptservlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Make sure you set this in your environment or via your container's config
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // allow testing via browser: /chat?message=hello
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // ensure proper encoding
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");

        if (OPENAI_API_KEY == null || OPENAI_API_KEY.isBlank()) {
            resp.sendError(500, "OpenAI API key not configured");
            return;
        }

        String userMsg = req.getParameter("message");
        if (userMsg == null) userMsg = "";

        // 1) build the JSON payload
        JSONObject payload = new JSONObject()
            .put("model", "gpt-3.5-turbo")
            .put("messages", new JSONArray()
                .put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You are a helpful assistant."))
                .put(new JSONObject()
                    .put("role", "user")
                    .put("content", userMsg))
            );

        // 2) create the HTTP request
        HttpRequest httpReq = HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + OPENAI_API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        // 3) send it
        HttpResponse<String> httpRes;
        try {
            httpRes = HttpClient.newHttpClient()
                                .send(httpReq, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException(e);
        }

        // 4) if OpenAI returned an error status, forward it
        if (httpRes.statusCode() != 200) {
            resp.setStatus(httpRes.statusCode());
            resp.setContentType("application/json; charset=UTF-8");
            resp.getWriter().write(httpRes.body());
            return;
        }

        // 5) parse out the assistant’s reply
        JSONObject json = new JSONObject(httpRes.body());
        String reply = json
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim();

        // 6) return it as JSON
        resp.setStatus(200);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(new JSONObject().put("reply", reply).toString());
    }
}