#!/bin/bash

set -e

workDir=~/Téléchargements
targetFile=../src/main/resources/META-INF/native-image/reflect-config.json

filter1='io.fabric8.kubernetes.api.model'
filter2='io.fabric8.kubernetes.client.*CustomResource'
filter3='istio.api.networking.*'
filter4='Serializer|Deserializer'
filter="${filter1}|${filter2}|${filter3}|${filter4}"

cat <<EOF > $targetFile
[
  {"name": "java.util.LinkedHashMap", "methods": [{ "name": "<init>", "parameterTypes": [] }]},
EOF

for jarGroupArtefactVersion in \
      io.javaoperatorsdk:operator-framework:1.7.5     \
      io.fabric8:kubernetes-model-common:5.1.1        \
      io.fabric8:kubernetes-model-core:5.1.1          \
      io.fabric8:kubernetes-model-networking:5.1.1    \
      io.fabric8:kubernetes-model-apiextensions:5.1.1 \
      io.fabric8:kubernetes-client:5.1.1              \
      me.snowdrop:istio-model:1.7.7.1                 \
      me.snowdrop:istio-client:1.7.7.1                \
    ; do

  jarGroup=$(echo ${jarGroupArtefactVersion} | awk -F':' '{print $1}')
  jarName=$(echo ${jarGroupArtefactVersion} | awk -F':' '{print $2}')
  jarVersion=$(echo ${jarGroupArtefactVersion} | awk -F':' '{print $3}')
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
  {"name": "io.neo9.ingress.access.config.AdditionalWatchersConfig", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.config.WatchIngressAnnotationsConfig", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.config.UpdateIstioIngressSidecarConfig", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.config.ExposerConfig", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpec", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpecSources", "allDeclaredMethods": true, "allPublicConstructors": true}
]
EOF

echo "Number of lines :"
wc -l ${targetFile}
