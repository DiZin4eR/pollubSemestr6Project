# Simple Indicator JSON Imports

These files contain simple, clean indicator subject data for manual import through the `/indicators` page.

Use the `Import JSON` form and select any file in this folder.

Files:

- `environment-indicators.json`
- `economy-indicators.json`
- `population-indicators.json`

Format:

```json
{
  "subjects": [
    {
      "id": "KIMP_ENV",
      "parentId": null,
      "name": "Environment indicators",
      "hasVariables": false,
      "children": ["KIMP_ENV_AIR"],
      "levels": [1]
    }
  ]
}
```

Notes:

- Each file contains only valid import data.
- There are no duplicate, invalid, update-specific, or edge-case records.
- IDs are prefixed with `KIMP_` so imported test data is easy to find in the indicators page.
