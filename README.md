# 🏦 AI-Powered Banking Document Assistant

This project is a full-stack **AI-powered banking dashboard** built using Java and Eclipse, allowing users to:

- Upload and verify financial documents
- Automatically classify document types (PDF/DOCX)
- Interact with an **OpenAI-powered chatbot** to summarize or query document contents

It streamlines onboarding workflows, reduces manual review time by over **40%**, and improves document intake efficiency.

---

## ✨ Key Features

✅ **Secure Document Upload**  
Users can upload financial documents (PDFs, DOCX) via an intuitive HTML interface.

✅ **AI Chatbot Integration**  
ChatGPT API allows querying and summarizing uploaded content for faster decision-making.

✅ **Automated Document Type Classification**  
System detects if the uploaded file is a `.pdf` or `.docx` and verifies format accordingly.

✅ **Automated Routing**  
Files are stored in a designated folder based on servlet-side path configuration (using Apache Tomcat).

✅ **Improved Workflow Efficiency**  
Reduced manual overhead for financial teams through intelligent automation.

---

## 🧠 Technologies Used

| Technology           | Purpose                                            |
|----------------------|----------------------------------------------------|
| Java (Eclipse)       | Core backend, servlet logic                        |
| HTML/CSS             | Frontend UI for uploading and interacting          |
| OpenAI API           | ChatGPT-based document assistant chatbot           |
| Apache Tomcat 11.0.7 | Server runtime for servlet deployment              |
| PDFBox               | Parsing and processing PDF documents               |
| org.json (JSON.jar)  | JSON handling for API responses and metadata       |

---

## 📁 Libraries Used

Make sure the following `.jar` files are included in your `/WEB-INF/lib/` directory:

- `json-20230227.jar`
- `pdfbox-app-3.0.5.jar`

These are required for JSON parsing and PDF manipulation respectively.

---

## ⚙️ Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/YourUsername/YourRepoName.git
cd YourRepoName
```

### 2️⃣ Setup Your Servlet Environment

Open the project in **Eclipse**.

Ensure **Apache Tomcat 11.0.7** is configured as the server.

Place the `.jar` files (`json-20230227.jar` and `pdfbox-app-3.0.5.jar`) in:

```bash
src/main/webapp/WEB-INF/lib
```

### 3️⃣ Update Upload Path

In `FileUploadServlet.java`, set the `uploadPath` variable to the folder where uploaded files should be stored:

```java
String uploadPath = "/absolute/path/to/your/upload/folder";
```

Make sure this path exists and is **writable** by the Tomcat process.

### 4️⃣ Run the Application

Start the **Apache Tomcat** server.

Access the app at:

```bash
http://localhost:8080/FileUploader/upload.html
```

---

### 🧠 How it Works

1. **Upload Document** → `.docx` or `.pdf`  
2. **Classify File Type** using extension + metadata  
3. **Extract Content** using Apache PDFBox / DOCX parser  
4. **Send Content to ChatGPT API** to generate summary  
5. **Display Summary**
