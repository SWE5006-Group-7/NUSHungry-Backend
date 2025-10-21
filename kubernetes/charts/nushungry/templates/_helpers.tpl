{{/*
Expand the name of the chart.
*/}}
{{- define "nushungry.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "nushungry.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "nushungry.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "nushungry.labels" -}}
helm.sh/chart: {{ include "nushungry.chart" . }}
{{ include "nushungry.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "nushungry.selectorLabels" -}}
app.kubernetes.io/name: {{ include "nushungry.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "nushungry.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "nushungry.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Return the proper image name
*/}}
{{- define "nushungry.image" -}}
{{- $registryName := .imageRoot.registry -}}
{{- $repositoryName := .imageRoot.repository -}}
{{- $tag := .imageRoot.tag | toString -}}
{{- if .global }}
    {{- if .global.imageRegistry }}
     {{- $registryName = .global.imageRegistry -}}
    {{- end -}}
{{- end -}}
{{- if $registryName }}
{{- printf "%s/%s:%s" $registryName $repositoryName $tag -}}
{{- else -}}
{{- printf "%s:%s" $repositoryName $tag -}}
{{- end -}}
{{- end -}}

{{/*
Return the proper namespace
*/}}
{{- define "nushungry.namespace" -}}
{{- if .Values.global }}
    {{- if .Values.global.namespace }}
        {{- .Values.global.namespace -}}
    {{- else -}}
        {{- .Values.namespace.name | default "nushungry" -}}
    {{- end -}}
{{- else -}}
    {{- .Values.namespace.name | default "nushungry" -}}
{{- end -}}
{{- end -}}

{{/*
Return the Eureka service URL
*/}}
{{- define "nushungry.eurekaUrl" -}}
http://eureka-server:8761/eureka/
{{- end -}}

{{/*
Return the Config Server URL
*/}}
{{- define "nushungry.configServerUrl" -}}
http://config-server:8888
{{- end -}}
