package com.mycompany.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

// PDFBox 3.x imports
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

@WebServlet("/upload")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 1,   // 1 MB
    maxFileSize       = 1024 * 1024 * 10,  // 10 MB per file
    maxRequestSize    = 1024 * 1024 * 50   // 50 MB total
)
public class FileUploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Directory on Desktop where uploads live
    private static final String UPLOAD_DIR =
        System.getProperty("user.home")
        + File.separator + "Desktop"
        + File.separator + "UploadedFiles";

    @Override
    public void init() throws ServletException {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // Show the upload form
        req.getRequestDispatcher("/upload.html").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = req.getParameter("action");
        if ("verify".equals(action)) {
            // ===== VERIFY MODE =====

            // List only real files (skip dot‐files)
            File dir = new File(UPLOAD_DIR);
            File[] files = dir.exists()
                ? dir.listFiles(f -> f.isFile() && !f.getName().startsWith("."))
                : new File[0];
            Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

            // Build a JS-array literal of {name,type,data/text}
            StringBuilder docs = new StringBuilder("[");
            for (File f : files) {
                String name  = f.getName();
                String lower = name.toLowerCase();

                if (lower.endsWith(".pdf")) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                    String b64   = java.util.Base64.getEncoder().encodeToString(bytes);
                    docs.append("{")
                        .append("name:'").append(escapeJs(name)).append("',")
                        .append("type:'pdf',")
                        .append("data:'").append(b64).append("'},");

                } else {
                    String text = "";
                    try (InputStream is = new FileInputStream(f)) {
                        if (lower.endsWith(".docx")) {
                            text = extractDocxText(is);
                        } else {
                            text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    }
                    docs.append("{")
                        .append("name:'").append(escapeJs(name)).append("',")
                        .append("type:'text',")
                        .append("text:'").append(escapeJs(text)).append("'},");

                }
                // Log raw to Eclipse console
                System.out.println("=== " + name + " ===");
            }
            docs.append("]");

            // Emit the verification HTML/CSS/JS, now with a 1–10 dropdown in top-left
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
                .append("<title>Verification</title>")
                .append("<style>")
                  // page background
                  .append("body{margin:0;font-family:sans-serif;background:#001f3f;}")

                  // beige card
                  .append("#verify{background:#f5f5dc;width:90%;height:80vh;margin:2rem auto;"
                         + "padding:2rem;border-radius:8px;position:relative;overflow:auto;}")

                  // dropdown top-left
                  .append("#docSelect{position:absolute;top:1rem;left:1rem;"
                         + "padding:.5rem;font-size:1rem;}")

                  // file title
                  .append("#fileName{margin:0 0 1rem;color:#001f3f;font-size:1.5rem;text-align:center;}")

                  // content pane
                  .append("#fileContent{background:#fff;white-space:pre-wrap;border:1px solid #ddd;"
                         + "border-radius:4px;padding:1rem;"
                         + "height:calc(100% - 4rem);overflow:auto;}")

                  // Yes/No controls
                  .append("#controls{position:absolute;top:1rem;right:1rem;}")
                  .append("#controls button{margin-left:0.5rem;padding:0.75rem 1.5rem;"
                         + "font-size:1rem;border:none;border-radius:4px;cursor:pointer;}")  
                  .append("#yesBtn{background:#4CAF50;color:#fff;}#yesBtn:hover{background:#45A049;}")  
                  .append("#noBtn{background:#ff9800;color:#fff;}#noBtn:hover{background:#e68a00;}")

                  // submit button (fixed bottom-right)
                  .append("#submitBtn{display:none;position:fixed;bottom:1rem;right:1rem;"
                         + "padding:0.75rem 1.5rem;font-size:1rem;border:none;border-radius:4px;"
                         + "background:#007BFF;color:#fff;cursor:pointer;}")  
                  .append("#submitBtn:hover{background:#0069D9;}")
                .append("</style>")
              .append("</head><body>")

                // verification card
                .append("<div id='verify'>")

                  // NEW: dropdown 1–10
                  .append("<select id='docSelect'>");
            for (int i = 1; i <= 10; i++) {
                html.append("<option value='").append(i).append("'>").append(i).append("</option>");
            }
            html.append("</select>")

                  // file name + content
                  .append("<h2 id='fileName'></h2>")
                  .append("<div id='fileContent'></div>")

                  // Yes/No INSIDE
                  .append("<div id='controls'>")
                    .append("<button id='yesBtn'>Yes</button>")
                    .append("<button id='noBtn'>No</button>")
                  .append("</div>")

                .append("</div>") // end #verify

                // Submit button OUTSIDE
                .append("<button id='submitBtn'>Submit</button>")

                // stepping logic
                .append("<script>")
                  .append("const docs=").append(docs).append(";let idx=0;\n")
                  .append("function render(i){")
                    .append("let f=docs[i];")
                    .append("document.getElementById('fileName').textContent=f.name;")
                    .append("let slot=document.getElementById('fileContent');")
                    .append("if(f.type==='pdf'){")
                      .append("slot.innerHTML=`<object data='data:application/pdf;base64,${f.data}' "
                             + "type='application/pdf' width='100%' height='100%'></object>`;")
                    .append("} else {")
                      .append("slot.textContent=f.text;")
                    .append("}")
                    // show/hide Yes/No vs Submit
                    .append("if(i===docs.length-1){")
                      .append("controls.style.display='none';")
                      .append("submitBtn.style.display='block';")
                    .append("}else{")
                      .append("controls.style.display='block';")
                      .append("submitBtn.style.display='none';")
                    .append("}")
                  .append("}\n")
                  .append("function next(){ if(idx<docs.length-1){ idx++; render(idx);} }\n")
                  .append("yesBtn.onclick=next; noBtn.onclick=next;\n")
                  .append("submitBtn.onclick=_=>{")
                    .append("document.body.innerHTML=")
                      .append("'<h2 style=\"color:#f5f5dc;text-align:center;margin-top:2rem;\">'"
                        + "+ 'Documents are all verified.';")
                  .append("};\n")
                  .append("if(docs.length>0) render(0); else document.body.innerHTML=")
                    .append("'<h2 style=\"color:#f5f5dc;text-align:center;margin-top:2rem;\">'"
                      + "+'No files to verify.';")
                .append("</script>")

              .append("</body></html>");

            resp.setContentType("text/html");
            resp.getWriter().write(html.toString());
            return;
        }

        // ===== UPLOAD MODE =====
        for (Part part : req.getParts()) {
            if (!"files".equals(part.getName()) || part.getSize()==0) continue;
            String fn = Paths.get(part.getSubmittedFileName()).getFileName().toString();
            part.write(UPLOAD_DIR + File.separator + fn);
        }
        resp.setContentType("text/html");
        resp.getWriter().println(
          "<h2>Upload successful!</h2>" +
          "<p>Saved to: "+ UPLOAD_DIR +"</p>"
        );
    }

    // .docx → unzip & pull <w:t> nodes
    private String extractDocxText(InputStream docxStream) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(docxStream)) {
            ZipEntry e;
            while ((e=zis.getNextEntry())!=null) {
                if ("word/document.xml".equals(e.getName())) {
                    Document d = DocumentBuilderFactory.newInstance()
                                   .newDocumentBuilder().parse(zis);
                    NodeList nl = d.getElementsByTagName("w:t");
                    StringBuilder sb = new StringBuilder();
                    for (int i=0; i<nl.getLength(); i++)
                        sb.append(nl.item(i).getTextContent());
                    return sb.toString();
                }
            }
        } catch (Exception ex) {
            throw new IOException("Failed to extract .docx text", ex);
        }
        return "";
    }

    // JS-string escaper
    private static String escapeJs(String s) {
        return s.replace("\\","\\\\")
                .replace("'","\\'")
                .replace("\r","")
                .replace("\n","\\n");
    }
}
