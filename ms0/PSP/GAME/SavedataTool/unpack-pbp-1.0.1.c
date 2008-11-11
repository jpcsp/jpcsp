//
// unpack-pbp-1.0.1.c - Dan Peori (peori@oopo.net)
// Copy all you want. Please give me some credit.
//
// v1.0.0 - 01/09/2005: Initial release.
// v1.0.1 - 04/01/2005: Big endian support. (rpineau)
//
// This program will unpack a PSP firmware update package into its separate
// files. As this is very early in the PSP's life there is some guesswork as
// to the filenames and general structure. As well, we don't know what some
// of the files are. If you have any information, please feel free to email
// me, or to join the forums at ps2dev.org. Thanks!
//

 #include <stdio.h>
 #include <malloc.h>

#ifdef __BIG_ENDIAN__

 #include <machine/byte_order.h>

#endif 

 typedef struct { char signature[4]; int version; int offset[8]; } HEADER;

 char *filename[8] = { "PARAM.SFO", "ICON0.PNG", "ICON1.PMF", "UKNOWN.PNG",
                       "PIC1.PNG", "SND0.AT3", "UNKNOWN.PSP", "UNKNOWN.PSAR" };

 int main(int argc, char *argv[]) {
  FILE *infile, *outfile; HEADER header; int loop0, total_size;

  // Check the argument count.
  if (argc != 2) { printf("USAGE: %s <filename>\n", argv[0]); return -1; }

  // Open the input file.
  infile = fopen(argv[1], "rb");
  if (infile == NULL) { printf("ERROR: Could not open the input file. (%s)\n", argv[1]); return -1; }

  // Get the input file size.
  fseek(infile, 0, SEEK_END); total_size = ftell(infile); fseek(infile, 0, SEEK_SET);
  if (total_size < 0) { printf("ERROR: Could not get the input file size.\n"); return -1; }

  // Read in the input file header.
  if (fread(&header, sizeof(HEADER), 1, infile) < 0) { printf("ERROR: Could not read the input file header.\n"); return -1; }

  // Check the input file signature.
  if (header.signature[0] != 0x00) { printf("ERROR: Input file is not a PBP file.\n"); return -1; } else
  if (header.signature[1] != 0x50) { printf("ERROR: Input file is not a PBP file.\n"); return -1; } else
  if (header.signature[2] != 0x42) { printf("ERROR: Input file is not a PBP file.\n"); return -1; } else
  if (header.signature[3] != 0x50) { printf("ERROR: Input file is not a PBP file.\n"); return -1; }

#ifdef __BIG_ENDIAN__

  // Swap the byte order for big-endian machines.
  for (loop0=0; loop0<8; loop0++) { header.offset[loop0] = NXSwapInt(header.offset[loop0]); }

#endif 

  // For each section...
  for (loop0=0; loop0<8; loop0++) { void *buffer; int size;

   // Get the size of the last section data.
   if (loop0 == 7) { size = total_size - header.offset[loop0]; }

   // Get the size of the section data.
   else { size = header.offset[loop0 + 1] - header.offset[loop0]; }

   // Allocate the section data buffer.
   buffer = malloc(size);
   if (buffer == NULL) { printf("ERROR: Could not allocate the section data buffer. (%d)\n", size); return -1; }

   // Read in the section data.
   if (fread(buffer, size, 1, infile) < 0) { printf("ERROR: Could not read in the section data.\n"); return -1; }

   // Open the output file.
   outfile = fopen(filename[loop0], "wb");
   if (outfile == NULL) { printf("ERROR: Could not open the output file. (%s)\n", filename[loop0]); return -1; }

   // Write out the section data.
   if (fwrite(buffer, size, 1, outfile) < 0) { printf("ERROR: Could not write out the section data.\n"); return -1; }

   // Close the output file.
   if (fclose(outfile) < 0) { printf("ERROR: Could not close the output file.\n"); return -1; }

   // Free the section data buffer.
   free(buffer);

   // Output the section information.
   printf("[%d] %8d bytes | %s\n", loop0, size, filename[loop0]);

  }

  // Close the input file.
  if (fclose(infile) < 0) { printf("ERROR: Could not close the input file.\n"); return -1; }

  // End program.
  return 0;

 }
