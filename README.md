# 📄 Tabloid

**Tabloid** is a standalone RESTful microservice for transforming structured JSON data into multiple tabular file formats—including XLSX, ODS, CSV, HTML, and PDF/UA.

Built with **Java** and **Quarkus**, Tabloid is designed for simplicity, making it ideal for generating human- and machine-readable reports from structured data.

---

## 🚀 Features

- 🔁 **Single POST API**: One endpoint to rule them all.
- 📦 **Multi-format Output**: Convert JSON data into:
  - XLSX (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`)
  - ODS (`application/vnd.oasis.opendocument.spreadsheet`)
  - CSV (`text/csv`)
  - HTML (`text/html`)
  - PDF/UA (`application/pdf`)
- 🧠 **In-memory processing**: Lightweight and fast.
- 🧱 Built on **Quarkus**, with support from industry-standard libraries.

---

## 📬 API Usage

### Endpoint

Request:

```
POST /generate
Content-Type: application/json
Accept: [desired MIME type]
```

### Request Body (Example)

```json
{
  "tables": [
    {
      "name": "Sheet1",
      "columns": [
        { "name": "Name",     "type": "string" },
        { "name": "Age",      "type": "number" },
        { "name": "Birthday", "type": "date", "format": "yyyy-MM-dd" }
      ],
      "rows": [
        ["Alice", 30, "1993-01-01"],
        ["Bob",   25, "1998-05-23"],
        ["Cara",  40, "1983-09-12"]
      ]
    }
  ]
}
```

### Response

The response will be an octet stream in the format specified by the `Accept` request header and a `Content-Disposition` header defining the filename.

---

## 🛠️ Tech Stack

| Purpose              | Tool/Library                            |
|----------------------|------------------------------------------|
| Framework            | [Quarkus](https://quarkus.io)            |
| XLSX Generation      | [Apache POI (Quarkus extension)](https://quarkus.io/extensions/io.quarkiverse.poi/quarkus-poi/) |
| ODS Generation       | [ODF Toolkit](https://odftoolkit.org/)   |
| CSV Generation       | [Jackson CSV](https://github.com/FasterXML/jackson-dataformats-text) |
| HTML Rendering       | [Quarkus Qute](https://quarkus.io/guides/qute) |
| PDF Generation       | [openhtmltopdf](https://github.com/openhtmltopdf/openhtmltopdf) |

---

## 🧪 Development Status

**MVP Scope:**

- ✅ In-memory processing (no streaming).
- ✅ Blocking I/O.
- ✅ JSON input with schema.
- ✅ Separate handlers for each MIME type.
- ⏳ Test data to be provided.

**Out of Scope (for MVP):**

- ❌ Streaming I/O for large datasets.
- ❌ Native compilation with GraalVM.
- ❌ CSV/HTML/PDF as input formats.
- ❌ CLI support.

---

## 📈 Roadmap

- [ ] Add support for streaming I/O
- [ ] Implement CLI mode
- [ ] Add metadata (e.g., filename) in top-level JSON
- [ ] Enable native builds
- [ ] Expand input format support

---

## 🤝 Contributing

Pull requests and discussions are welcome. Please open an issue for significant changes or new features.

---

## 📝 License

[GPLv3](LICENSE)

---

## 👤 Author

Project maintained by the [ITBH](https://github.com/itbh-at) team.

