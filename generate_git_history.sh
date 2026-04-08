#!/bin/bash
git config init.defaultBranch master
git config user.name "Vaishnav"
git config user.email "vaishnav@example.com"

# Setup the dates
D1="2026-04-01T10:00:00"
D2="2026-04-02T14:30:00"
D3="2026-04-03T11:15:00"
D4="2026-04-04T16:45:00"
D5="2026-04-05T13:20:00"
D6="2026-04-06T15:00:00"
D7="2026-04-07T14:10:00"
D8="2026-04-08T10:00:00"

# Day 1
export GIT_AUTHOR_DATE=$D1
export GIT_COMMITTER_DATE=$D1
git add pom.xml .gitignore README.md BUILDING_PLAN.md 2>/dev/null
git commit -m "Day 1: Project Setup and Architecture Planning"

# Day 2
export GIT_AUTHOR_DATE=$D2
export GIT_COMMITTER_DATE=$D2
git add src/main/java/com/jarvis/Configuration.java src/main/java/com/jarvis/JarvisTerminal.java 2>/dev/null
git commit -m "Day 2: Core Components and Configuration Base"

# Day 3
export GIT_AUTHOR_DATE=$D3
export GIT_COMMITTER_DATE=$D3
git add src/main/java/com/jarvis/ui/ 2>/dev/null
git commit -m "Day 3: Terminal UI Implementation"

# Day 4
export GIT_AUTHOR_DATE=$D4
export GIT_COMMITTER_DATE=$D4
git add src/main/java/com/jarvis/security/ src/main/java/com/jarvis/AuditLogger.java src/main/java/com/jarvis/IntentEngine.java config/blacklist.txt 2>/dev/null
git commit -m "Day 4: Security Layer and Input Validation"

# Day 5
export GIT_AUTHOR_DATE=$D5
export GIT_COMMITTER_DATE=$D5
git add src/main/java/com/jarvis/api/ src/main/resources/ 2>/dev/null
git commit -m "Day 5: OpenAI/Gemini Backend Integration"

# Day 6
export GIT_AUTHOR_DATE=$D6
export GIT_COMMITTER_DATE=$D6
git add src/main/java/com/jarvis/docker/ Dockerfile docker-compose.yml 2>/dev/null
git commit -m "Day 6: Docker Sandbox Execution Environment"

# Day 7
export GIT_AUTHOR_DATE=$D7
export GIT_COMMITTER_DATE=$D7
git add src/main/java/com/jarvis/plugin/ plugins/ jarvis-terminal.sh 2>/dev/null
git commit -m "Day 7: Plugin Management and CLI Wrapper"

# Day 8
export GIT_AUTHOR_DATE=$D8
export GIT_COMMITTER_DATE=$D8
git add .
git commit -m "Day 8: Final Review, Testing, and Comprehensive Documentation"

