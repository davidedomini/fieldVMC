#!/usr/bin/env sh
DESTINATION="$HOME/Downloads/vmc-basic-example-$(date --utc "+%F-%H.%M.%S")"
git clone https://github.com/angelacorte/vmc-experiments.git "$DESTINATION"
cd "$DESTINATION"
./gradlew runOneRootGraphic