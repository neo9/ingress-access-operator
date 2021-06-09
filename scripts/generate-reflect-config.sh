#!/bin/bash

set -e

workDir=~/Téléchargements
targetFile=../src/main/resources/META-INF/native-image/reflect-config.json

filter1='io.fabric8.kubernetes.api.model'
filter2='io.fabric8.kubernetes.client.*CustomResource'
filter3='Serializer|Deserializer'
filter="${filter1}|${filter2}|${filter3}"

cat <<EOF > $targetFile
[
  {
    "name": "java.util.LinkedHashMap",
    "methods": [
      { "name": "<init>", "parameterTypes": [] }
    ]
  },
EOF

for jarGroupArtefactVersion in \
      io.javaoperatorsdk_operator-framework_1.8.4     \
      io.fabric8_kubernetes-model-common_5.3.1        \
      io.fabric8_kubernetes-model-core_5.3.1          \
      io.fabric8_kubernetes-model-networking_5.3.1    \
      io.fabric8_kubernetes-model-apiextensions_5.3.1 \
      io.fabric8_kubernetes-client_5.3.1              \
    ; do

  jarGroup=$(echo ${jarGroupArtefactVersion} | awk -F'_' '{print $1}')
  jarName=$(echo ${jarGroupArtefactVersion} | awk -F'_' '{print $2}')
  jarVersion=$(echo ${jarGroupArtefactVersion} | awk -F'_' '{print $3}')
  jarFileName=${jarName}-${jarVersion}.jar

  if [ ! -f "${workDir}/${jarFileName}" ]; then
    cd ${workDir}
    wget "https://repo1.maven.org/maven2/$(echo "${jarGroup}" | tr '.' '/')/${jarName}/${jarVersion}/${jarFileName}"
    cd -
  fi

  unzip -l "${workDir}/${jarFileName}" \
    | grep '.class' \
    | awk '{print $4}' \
    | sed 's/.class$//' \
    | tr '/' '.' \
    | grep -Ev '\.$' \
    | grep -E ${filter} \
    | grep -Ev 'Fluent|Builder' \
    | while read l; do
        echo "  {\"name\": \"$l\", \"allDeclaredMethods\": true, \"allPublicConstructors\": true},";
      done >> $targetFile

    echo "${jarName} ... done"
done

cat <<EOF >> $targetFile
  {
    "name": "io.neo9.gatekeeper.customresources.spec.V1VisitorGroupSpec",
    "allDeclaredMethods": true,
    "allPublicConstructors": true
  },
  {
    "name": "io.neo9.gatekeeper.customresources.spec.V1VisitorGroupSpecSources",
    "allDeclaredMethods": true,
    "allPublicConstructors": true
  }
]
EOF

echo "Number of lines :"
wc -l ${targetFile}
