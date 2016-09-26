#!/bin/bash

if (( $# < 1 ))
then
    echo "Usage: $0 <url> <file path> <file name>"
    echo "example: $0 http://localhost/content sling-log.png foo-1.png"
    exit 1
fi

file_name="$3"
url="$1"
file_path=$2
cred="${4:-admin:admin}"
temp_file=$(mktemp)
cat << EOF > "$temp_file"
  {
  	"jcr:primaryType": "at16:Asset",
  	"jcr:content": {
  		"jcr:primaryType": "oak:Unstructured",
  		"name": "$file_name",
  		"renditions": {
  			"jcr:primaryType": "nt:folder"
  		}
  	}
  }
EOF
curl -u "$cred" -F":operation=import" -F":contentType=json"  -F":replace=true" -F":replaceProperties=true" \
                -F":contentFile=@$temp_file" \
                -F":name=$file_name" \
                "$url"
curl -X POST -u "$cred" -F"@TypeHint=nt:file" -F"original=@$file_path" "$url/$file_name/jcr:content/renditions" 

rm "$temp_file"
