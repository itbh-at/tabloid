# 📑 Tabloid API Guide

This guide provides a comprehensive reference for all format-specific options supported by the Tabloid API. Use these options in your JSON payload to customize the output for XLSX, ODS, CSV, HTML, and PDF formats.

---

## Table of Contents

- [Overview](https://www.google.com/search?q=%23overview)
- [Request Structure](https://www.google.com/search?q=%23request-structure)
- [Column Alignment](https://www.google.com/search?q=%23column-alignment)
- [Format-Specific Options](https://www.google.com/search?q=%23format-specific-options)
  - [XLSX Options](https://www.google.com/search?q=%23xlsx-options)
  - [ODS Options](https://www.google.com/search?q=%23ods-options)
  - [CSV Options](https://www.google.com/search?q=%23csv-options)
  - [HTML Options](https://www.google.com/search?q=%23html-options)
  - [PDF Options](https://www.google.com/search?q=%23pdf-options)
- [Examples](https://www.google.com/search?q=%23examples)

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
    {
      "name": "Sheet1",
      "columns": [
        { "name": "Name", "type": "string" },
        {
          "name": "Age",
          "type": "number",
          "alignment": { "horizontal": "right" }
        },
        { "name": "Birthday", "type": "date", "format": "yyyy-MM-dd" }
      ],
      "rows": [
        ["Alice", 30, "1993-01-01"],
        ["Bob", 25, "1998-05-23"],
        ["Cara", 40, "1983-09-12"]
      ]
    }
  ]
}
```

---

## Column Alignment

You can specify the horizontal and vertical alignment of content within a column by adding an `alignment` object to a column definition.

| Property     | Type   | Description                                             |
| ------------ | ------ | ------------------------------------------------------- |
| `horizontal` | string | Horizontal alignment. Can be `left`, `center`, `right`. |
| `vertical`   | string | Vertical alignment. Can be `top`, `middle`, `bottom`.   |

### Example

```json
"columns": [
  {
    "name": "Region",
    "type": "string",
    "alignment": {
      "horizontal": "center",
      "vertical": "middle"
    }
  }
]
```

---

## Format-Specific Options

### XLSX Options

Set `format: "xlsx"` in the `formats` array. All fields are optional.

| Option             | Type    | Default | Description                                  |
| ------------------ | ------- | ------- | -------------------------------------------- |
| freezeHeaderRow    | boolean | false   | Freeze the first row (header) in each sheet. |
| freezeHeaderColumn | boolean | false   | Freeze the first column in each sheet.       |
| hasHeaderColumn    | boolean | false   | Style the first column as a header.          |
| hasFooterRow       | boolean | false   | Style the last row as a footer.              |
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
| hasFooterRow       | boolean | false   | Treat the last row as a footer.        |

---

### ODS Options

Set `format: "ods"` in the `formats` array. All fields are optional.

| Option          | Type    | Default | Description                         |
| --------------- | ------- | ------- | ----------------------------------- |
| hasHeaderColumn | boolean | false   | Style the first column as a header. |
| hasFooterRow    | boolean | false   | Style the last row as a footer.     |
| hasHeaderRow    | boolean | false   | Style the first row as a header.    |
| hasFooterColumn | boolean | false   | Style the last column as a footer.  |

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

| Option          | Type    | Default               | Description                         |
| --------------- | ------- | --------------------- | ----------------------------------- |
| css             | string  | classpath:default.css | Path to CSS file for styling.       |
| hasHeaderColumn | boolean | false                 | Style the first column as a header. |
| hasFooterRow    | boolean | false                 | Style the last row as a footer.     |
| hasHeaderRow    | boolean | false                 | Style the first row as a header.    |
| hasFooterColumn | boolean | false                 | Style the last column as a footer.  |

---

### PDF Options

Set `format: "pdf"` in the `formats` array.

| Option          | Type    | Default | Description                         |
| --------------- | ------- | ------- | ----------------------------------- |
| version         | number  | —       | PDF version (e.g., 1.7).            |
| size            | string  | —       | Page size (e.g., A4, Letter).       |
| hasHeaderColumn | boolean | false   | Style the first column as a header. |
| hasFooterRow    | boolean | false   | Style the last row as a footer.     |

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

For more details on the API and request structure, see the main [README.md](https://www.google.com/search?q=./README.md).
