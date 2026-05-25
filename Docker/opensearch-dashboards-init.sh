#!/bin/sh
set -eu

DASHBOARDS_URL="${DASHBOARDS_URL:-http://opensearch-dashboards:5601}"
DASHBOARDS_USERNAME="${DASHBOARDS_USERNAME:-admin}"
DASHBOARDS_PASSWORD="${DASHBOARDS_PASSWORD:-123456789}"
INDEX_PATTERN_ID="${INDEX_PATTERN_ID:-application-logs}"
INDEX_PATTERN_TITLE="${INDEX_PATTERN_TITLE:-application-logs-*}"
INDEX_PATTERN_TIME_FIELD="${INDEX_PATTERN_TIME_FIELD:-@timestamp}"

curl_dashboards() {
  curl -fsS -u "${DASHBOARDS_USERNAME}:${DASHBOARDS_PASSWORD}" -H "osd-xsrf: true" "$@"
}

echo "Waiting for OpenSearch Dashboards at ${DASHBOARDS_URL}..."
until curl_dashboards "${DASHBOARDS_URL}/api/status" | grep -Eq '"state":"green"|"level":"available"'; do
  sleep 5
done

index_pattern_status="$(curl -s -o /dev/null -w "%{http_code}" -u "${DASHBOARDS_USERNAME}:${DASHBOARDS_PASSWORD}" -H "osd-xsrf: true" "${DASHBOARDS_URL}/api/saved_objects/index-pattern/${INDEX_PATTERN_ID}")"
if [ "${index_pattern_status}" = "404" ]; then
  index_pattern_payload="$(printf '{"attributes":{"title":"%s","timeFieldName":"%s"}}' "${INDEX_PATTERN_TITLE}" "${INDEX_PATTERN_TIME_FIELD}")"
  curl_dashboards -H "Content-Type: application/json" -X POST "${DASHBOARDS_URL}/api/saved_objects/index-pattern/${INDEX_PATTERN_ID}" --data-raw "${index_pattern_payload}" >/dev/null
  echo "Created index pattern ${INDEX_PATTERN_TITLE}."
else
  echo "Index pattern ${INDEX_PATTERN_ID} already exists."
fi

status_payload="$(curl_dashboards "${DASHBOARDS_URL}/api/status")"
dashboards_version="$(printf '%s' "${status_payload}" | sed -n 's/.*"number":"\([^"]*\)".*/\1/p' | head -n 1)"
dashboards_build_num="$(printf '%s' "${status_payload}" | sed -n 's/.*"build_number":\([0-9][0-9]*\).*/\1/p' | head -n 1)"

if [ -z "${dashboards_version}" ] || [ -z "${dashboards_build_num}" ]; then
  echo "Failed to detect OpenSearch Dashboards version/build number."
  exit 1
fi

config_payload="$(printf '{"attributes":{"buildNum":%s,"defaultIndex":"%s"}}' "${dashboards_build_num}" "${INDEX_PATTERN_ID}")"
curl_dashboards -H "Content-Type: application/json" -X POST "${DASHBOARDS_URL}/api/saved_objects/config/${dashboards_version}?overwrite=true" --data-raw "${config_payload}" >/dev/null
echo "Set default index pattern to ${INDEX_PATTERN_ID}."
