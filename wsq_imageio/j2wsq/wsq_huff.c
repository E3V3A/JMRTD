/***********************************************************************
      LIBRARY: WSQ - Grayscale Image Compression

      FILE:    HUFF.C
      AUTHORS: Craig Watson
               Michael Garris
      DATE:    12/02/1999

      Checks that huffman codes are WSQ compliant. The specification
      does not allow for an all 1's code in the code table.

      ROUTINES:
#cat: check_huffcodes_wsq - Checks for an all 1's code in the code table.

***********************************************************************/

#include <stdio.h>
#include <wsq.h>

int check_huffcodes_wsq(HUFFCODE *hufftable, int last_size)
{
   int i, k;
   int all_ones;

   for(i = 0; i < last_size; i++){
      all_ones = 1;
      for(k = 0; (k < (hufftable+i)->size) && all_ones; k++)
         all_ones = (all_ones && (((hufftable+i)->code >> k) & 0x0001));
      if(all_ones) {
         fprintf(stderr, "WARNING: A code in the hufftable contains an ");
         fprintf(stderr, "all 1's code.\n         This image may still be ");
         fprintf(stderr, "decodable.\n         It is not compliant with ");
         fprintf(stderr, "the WSQ specification.\n");
         return(-1);
      }
   }
   return(0);
}
