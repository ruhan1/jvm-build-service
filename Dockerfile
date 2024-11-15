# Build the manager binary
FROM registry.access.redhat.com/ubi9/go-toolset:1.21.13-2.1729776560@sha256:97e30a01caeece72ee967013e7c7af777ea4ee93840681ddcfe38a87eb4c084a as builder

# Copy the Go Modules manifests
COPY go.mod go.mod
COPY vendor/ vendor/
COPY pkg/ pkg/
COPY cmd/ cmd/

# Build
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 GOTOOLCHAIN=auto GOSUMDB=sum.golang.org go build -o jvmbuildservice cmd/controller/main.go

# Use ubi-minimal as minimal base image to package the manager binary
# Refer to https://catalog.redhat.com/software/containers/ubi8/ubi-minimal/5c359a62bed8bd75a2c3fba8 for more details
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.4-1227.1726694542@sha256:c0e70387664f30cd9cf2795b547e4a9a51002c44a4a86aa9335ab030134bf392
COPY --from=builder /opt/app-root/src/jvmbuildservice /
USER 65532:65532

ENTRYPOINT ["/jvmbuildservice"]
