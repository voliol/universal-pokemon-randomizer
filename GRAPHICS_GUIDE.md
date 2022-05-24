This is a technical guide/overview of the framework for images and palettes included in
the Universal Pokémon Randomizer. 

Please update this as you make changes to the framework, or remove it from your branch 
so that it doesn't contain outdated information. Prioritize self-documenting code (good code structure, 
well-named methods, classes and fields) and JavaDocs over this file.

# Overview

The framework consists of the graphics package, mostly for palettes as of yet,
as well as the older GFXFunctions class, and compressor/decompressor classes.

The graphics package classes have basic JavaDocs, the others do not.

Images can be read to some extent in all generations, but written only in Gen III. 

Palettes can be read and written in all generations.

# Images

The word "image" should be used generally, rather than "sprite". "Sprite" has a meaning within 
the consoles the ROMs are for, as an image that is not a background tile, and many of these methods 
work on those as well.

Images are used by the Randomizer GUI when a ROM is loaded, to display a "mascot" Pokémon, 
and there are methods for dumping all Pokémon images for bugtesting purposes.

Gen I uses Gen1Decmp to decompress "sprites". Whether background tiles can be decompressed as well is unclear.
This is only used for the mascot image, and image dumps. There are no corresponding methods to compress, 
so they could be written.

Gen II uses Gen2Decmp to decompress "sprites". Whether background tiles can be decompressed as well is unclear.
This is only used for the mascot image, and image dumps. There are no corresponding methods to compress, 
so they could be written.

Gen III uses DSDecmp to decompress images. (when needed, e.g. Pokémon icons are uncompressed in the ROM)
As there is a DScmp class, these can be compressed and written to ROM. No specific methods exist,
but Gen3RomHandler.rewriteCompressedData can be used when the pointer offset(s) for the image are known.
 
Gen IV and V have file systems, with Pokémon images in the "PokémonGraphics" NARC-file. It should be opened 
beforehand for faster mass operations. Then DSDecmp is used for decompression. The DScmp class 
could be used for compression, but the methods for inserting into the NARCs don't exist.

GFXFunctions also has a method, readTiledImageData, that reads a BufferedImage, and converts it to Gen III-V 
image data which can then be written. This method was originally written for the Palette framework, 
to fix the weird Blastoise sprite in FRLG (when palettes are randomized). But as I realized the 
surrounding framework (i.e. all missing writing above) would be substantial the method is unused for now.
This altered Blastoise image file can be found in graphics/resources as "pokeImageFRLG_09f.png", 
and can be used as an example for how images should be (i.e. 4bpp .png files, irfanview is the recommended image editor).


# Palettes
Palettes are read/written by the generation/platform-specific RomHandlers (they can be read/written in all generations), 
and then other palette operations are done in the PaletteHandlers.

The PaletteHandlers for Gen I and II are simple, and their implementations are made for the UPR.

The PaletteHandler for Gen III to V is shared between those generations, and is based on/a port of the 
Palette Randomization system in Artemis251's Emerald Randomizer.
Each game uses a .txt file from graphics/resources for the Pokémon palettes, 
and similar files could be used for other palettes. 
The syntax for these files is explained in the JavaDoc for ParsedDescription. 

The Palette class has a function for reading from a BufferedImage with an indexed palette. 