#!/bin/bash
curl -s -X POST http://localhost:8080/api/video/analyze \
  -H "Content-Type: application/json" \
  -d "{\"url\": \"$1\"}" \
  --max-time 60 | python3 -m json.tool
