#!/bin/bash

set -e

workDir=~/Téléchargements
targetFile=../src/main/resources/META-INF/native-image/reflect-config.json

filter1='io.fabric8.kubernetes.api.model'
filter2='io.fabric8.kubernetes.client.CustomResource|io.fabric8.kubernetes.client.*CustomResource'
filter3='Serializer|Deserializer'
filter4='io.fabric8.kubernetes.client.impl.*'
filter5='io.javaoperatorsdk.operator.processing'
filter="${filter1}|${filter2}|${filter3}|${filter4}|${filter5}"


exclusionFilterFabric8="io.fabric8.kubernetes.api.model.networking.v1beta1|io.fabric8.kubernetes.api.model.apiextensions.v1beta1"
exclusionFilter="${exclusionFilterFabric8}"

cd ..
javaoperatorsdkVersion=$(./gradlew dependencyInsight --configuration annotationProcessor --dependency io.javaoperatorsdk:operator-framework | grep -A1 ':dependencyInsight' | tail -1 | awk -F: '{print $NF}' | awk '{print $1}')
fabric8Version=$(./gradlew dependencyInsight --configuration annotationProcessor --dependency io.fabric8:kubernetes-client | grep -A1 ':dependencyInsight' | tail -1 | awk -F: '{print $NF}' | awk '{print $1}')
echo "detected javaoperatorsdkVersion=${javaoperatorsdkVersion}"
echo "detected fabric8Version=${fabric8Version}"
cd -

cat <<EOF > $targetFile
[
  {"name": "java.util.LinkedHashMap", "methods": [{ "name": "<init>", "parameterTypes": [] }]},
  {"name": "java.util.TreeMap", "methods": [{ "name": "<init>", "parameterTypes": [] }]},
  {"name": "java.util.concurrent.ThreadPoolExecutor", "allDeclaredMethods": true, "allPublicConstructors": true},
EOF

for jarGroupArtefactVersion in \
      io.javaoperatorsdk:operator-framework:${javaoperatorsdkVersion}      \
      io.javaoperatorsdk:operator-framework-core:${javaoperatorsdkVersion} \
      io.fabric8:kubernetes-model-common:${fabric8Version}                 \
      io.fabric8:kubernetes-model-core:${fabric8Version}                   \
      io.fabric8:kubernetes-model-networking:${fabric8Version}             \
      io.fabric8:kubernetes-model-apiextensions:${fabric8Version}          \
      io.fabric8:kubernetes-client:${fabric8Version}                       \
      io.fabric8:kubernetes-client-api:${fabric8Version}                   \
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

  completeExclusionFilter='Fluent|Builder'
  if [ ! -z "${exclusionFilter}" ]; then
    completeExclusionFilter="${completeExclusionFilter}|${exclusionFilter}"
  fi

  unzip -l "${workDir}/${jarFileName}" \
    | grep '.class' \
    | awk '{print $4}' \
    | sed 's/.class$//' \
    | tr '/' '.' \
    | grep -Ev '\.$' \
    | grep -E ${filter} \
    | grep -Ev ${completeExclusionFilter} \
    | while read l; do
        echo "  {\"name\": \"$l\", \"allDeclaredMethods\": true, \"allPublicConstructors\": true},";
      done >> $targetFile

    echo "${jarName} ... done"
done

cat <<EOF >> $targetFile
  {"name": "io.neo9.ingress.access.config.AdditionalWatchersConfig", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.config.UpdateIstioIngressSidecarConfig", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.config.ExposerConfig", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.config.DefaultFilteringConfig", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.config.AwsIngressConfig", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.customresources.VisitorGroup", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.customresources.VisitorGroupList", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpec", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpecSources", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.customresources.external.istio.Sidecar", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.customresources.external.istio.spec.SidecarSpec", "allDeclaredMethods": true, "allPublicConstructors": true},
  {"name": "io.neo9.ingress.access.customresources.external.istio.spec.EgressSpec", "allDeclaredMethods": true, "allPublicConstructors": true}
]
EOF

echo "Number of lines :"
wc -l ${targetFile}
