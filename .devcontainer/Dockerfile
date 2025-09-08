FROM thyrlian/android-sdk:latest

# Create and set permissions for workspace
RUN mkdir -p /workspace/.gradle && \
    chmod -R 777 /workspace/.gradle

# Additional VS Code Dev Container specific setup + curl for opencode
RUN apt-get update && apt-get install -y \
    git \
    openssh-client \
    make \
    android-tools-adb \
    curl \
    && apt-get clean

# Install opencode using official script
RUN curl -fsSL https://opencode.ai/install | bash

# Use the provided helper script to accept licenses
RUN /opt/license-accepter.sh

WORKDIR /workspace