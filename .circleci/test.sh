#!/bin/bash

run()
{
  case=$1
  workflow=$2
  echo "case: ${case}"
  digdag run --no-save -a ${workflow} \
    -p repos=${PWD}/../build/repo \
    -p token=${TOKEN} \
    -p version=${version}
}

run "success" "success.dig"

run "webhook_url validation" "validate_webhook_url.dig"
if [[ $? -eq 0 ]]; then
  exit 1
fi

run "template validation" "validate_template.dig"
if [[ $? -eq 0 ]]; then
  exit 1
else
  exit 0
fi
