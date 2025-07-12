# 📑 Tabloid API Guide

This guide provides a comprehensive reference for all format-specific options supported by the Tabloid API. Use these options in your JSON payload to customize the output for XLSX, ODS, CSV, HTML, and PDF formats.

---

## Table of Contents

- [Overview](#overview)
- [Request Structure](#request-structure)
- [Format-Specific Options](#format-specific-options)
  - [XLSX Options](#xlsx-options)
  - [ODS Options](#ods-options)
  - [CSV Options](#csv-options)
  - [HTML Options](#html-options)
  - [PDF Options](#pdf-options)
- [Examples](#examples)

---

## Overview

Tabloid accepts a JSON payload describing your document and tables. You can specify format-specific options in the `document.formats` array. Each object in this array configures one output format.

## Request Structure

```json
{
  "version": "1.0",
  "document": {
    "title": "Q3 2025 Sales Report",
    "author": "Jane Doe",
    "subject": "Quarterly Sales",
    "keywords": ["sales", "Q3", "2025"],
    "formats": [
      { "format": "xlsx", "freezeHeaderRow": true },
      { "format": "csv", "delimiter": ";", "charset": "UTF-8" },
      { "format": "html", "css": "classpath:default.css" }
    ]
  },
  "tables": [
    // ... tables as in the main README ...
  ]
}
```

---

## Format-Specific Options

### XLSX Options

Set `format: "xlsx"` in the `formats` array. All fields are optional.

| Option             | Type    | Default | Description                                  |
| ------------------ | ------- | ------- | -------------------------------------------- |
| freezeHeaderRow    | boolean | false   | Freeze the first row (header) in each sheet. |
| freezeHeaderColumn | boolean | false   | Freeze the first column in each sheet.       |
| hasHeaderColumn    | boolean | false   | Mark the first column as a header column.    |
| hasFooterColumn    | boolean | false   | Mark the last column as a footer column.     |
| hasFooterRow       | boolean | false   | Mark the last row as a footer row.           |
| tables             | object  | —       | Per-table options (see below).               |

#### Per-Table XLSX Options

Set per-table options by table name:

```json
{
  "format": "xlsx",
  "tables": {
    "Sheet1": {
      "freezeHeaderRow": true,
      "freezeHeaderColumn": true,
      "hasFooterRow": false
    }
  }
}
```

| Option             | Type    | Default | Description                            |
| ------------------ | ------- | ------- | -------------------------------------- |
| freezeHeaderRow    | boolean | false   | Freeze the first row in this table.    |
| freezeHeaderColumn | boolean | false   | Freeze the first column in this table. |
| hasFooterRow       | boolean | false   | Mark the last row as a footer row.     |

---

### ODS Options

_ODS format currently does not support per-request options via the JSON payload. Styling and behavior are controlled by server configuration._

---

### CSV Options

Set `format: "csv"` in the `formats` array. All fields are optional.

| Option                           | Type    | Default | Description                      |
| -------------------------------- | ------- | ------- | -------------------------------- |
| charset                          | string  | UTF-8   | Output file encoding.            |
| delimiter                        | string  | ,       | Field separator character.       |
| alwaysQuoteEmptyStrings          | boolean | true    | Always quote empty strings.      |
| alwaysQuoteStrings               | boolean | false   | Always quote all strings.        |
| escapeControlCharsWithEscapeChar | string  | —       | Escape char for control chars.   |
| escapeQuoteCharWithEscapeChar    | string  | —       | Escape char for quote chars.     |
| omitMissingTailColumns           | boolean | true    | Omit missing columns at row end. |
| trimValues                       | boolean | true    | Trim whitespace from values.     |
| includeHeader                    | boolean | true    | Include header row.              |

---

### HTML Options

Set `format: "html"` in the `formats` array.

| Option | Type   | Default               | Description                   |
| ------ | ------ | --------------------- | ----------------------------- |
| css    | string | classpath:default.css | Path to CSS file for styling. |

---

### PDF Options

Set `format: "pdf"` in the `formats` array.

| Option  | Type   | Default | Description                   |
| ------- | ------ | ------- | ----------------------------- |
| version | number | —       | PDF version (e.g., 1.7).      |
| size    | string | —       | Page size (e.g., A4, Letter). |

---

## Examples

### Example: XLSX with Per-Table Options

```json
{
  "version": "1.0",
  "document": {
    "title": "Multi-Sheet Report",
    "formats": [
      {
        "format": "xlsx",
        "tables": {
          "Sheet1": { "freezeHeaderRow": true },
          "Sheet2": { "freezeHeaderColumn": true, "hasFooterRow": true }
        }
      }
    ]
  },
  "tables": [
    /* ... */
  ]
}
```

### Example: CSV with Custom Delimiter

```json
{
  "version": "1.0",
  "document": {
    "title": "CSV Report",
    "formats": [{ "format": "csv", "delimiter": ";", "includeHeader": false }]
  },
  "tables": [
    /* ... */
  ]
}
```

---

For more details on the API and request structure, see the main [README.md](./tabloid/README.md).
