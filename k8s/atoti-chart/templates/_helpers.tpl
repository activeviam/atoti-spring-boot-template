{{/*
Expand the name of the chart.
*/}}
{{- define "atoti-chart.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
Truncated to 63 chars — Kubernetes DNS limit.
*/}}
{{- define "atoti-chart.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels applied to all resources — used for grouping and filtering.
*/}}
{{- define "atoti-chart.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/name: {{ include "atoti-chart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels — used by Deployments and Services to find matching Pods.
Must be a strict subset of labels and must not change after first deploy.
*/}}
{{- define "atoti-chart.selectorLabels" -}}
app.kubernetes.io/name: {{ include "atoti-chart.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
