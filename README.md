# Automatic clone extraction

This tool allows to automatically extract identical code snippets from Java programs.

## Requirements

* Python 3 (tested with Python 3.12)
* javalang (tested with version 0.13)

## Running the program

1) Automatic (requires CCStokener)

   Usage: `python3 main.py {path_to_file}`

   (If the file path is not specified in arguments, the program will ask to input it manually)

   This requires CCStokener to be placed in the same folder as this project's parent folder.

2) Manual (specifying the blocks)

   Usage: `python3 extractor.py {block1} {block2} ...`

   Blocks are specified in the format `XXX-YYY` (block of code from line `XXX` to line `YYY`, both inclusively)

   Again, if the program is run without arguments, you will be asked to input them through the console.