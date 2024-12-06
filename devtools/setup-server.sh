#!/bin/bash

# Define the PAPER_URL and destination directory
PAPER_URL="https://api.papermc.io/v2/projects/paper/versions/1.21.3/builds/80/downloads/paper-1.21.3-80.jar"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEST_DIR="$SCRIPT_DIR/../server"
PAPER_FILE="$DEST_DIR/paper.jar"
EULA_FILE="$DEST_DIR/eula.txt"
RUN_FILE="$DEST_DIR/run.sh"

# Create the destination directory if it doesn't exist
mkdir -p "$DEST_DIR"

# Check if paper.jar already exists
if [ ! -f "$PAPER_FILE" ]; then
  # Download paper.jar
  echo "Downloading paper.jar at $PAPER_FILE..."
  curl -o "$PAPER_FILE" "$PAPER_URL"
else
  echo "paper.jar already exists at $PAPER_FILE, skipping download."
fi

# Create eula.txt and set eula=true
echo "Creating eula.txt..."
echo "#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://aka.ms/MinecraftEULA)." > "$EULA_FILE"
echo "eula=true" >> "$EULA_FILE"

# Create run.sh with the specified command
echo "Creating run.sh..."
echo "#!/bin/bash" > "$RUN_FILE"
echo 'SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"' >> "$RUN_FILE"
echo 'cd $SCRIPT_DIR' >> "$RUN_FILE"
echo "java -DIReallyKnowWhatIAmDoingISwear -Xmx1024M -Xms1024M -jar paper.jar" >> "$RUN_FILE"
# Make run.sh executable
chmod +x "$RUN_FILE"

echo "Setup complete. Run ./server/run.sh to start the server."