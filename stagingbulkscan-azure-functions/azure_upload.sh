#!/bin/bash

export CONNECTION_STRING="DefaultEndpointsProtocol=http;AccountName=DUMMY;AccountKey=DUMMY;BlobEndpoint=http://127.0.0.1:10000/DUMMY;"
export CONTAINER_STORAGE="DUMMY"
export BLOB_NAME="DUMMY"

echo "Uploading file to blob container ${CONTAINER}"

az storage container create --connection-string ${CONNECTION_STRING} --name ${BLOB_NAME}
az storage blob upload --connection-string ${CONNECTION_STRING} -c ${BLOB_NAME} --name scan_documents.zip -f src/test/resources/scan_documents.zip