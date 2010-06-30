/***********************************************************************
      LIBRARY: FET - Feature File/List Utilities

      FILE:    FREEFET.C
      AUTHOR:  Michael Garris
      DATE:    01/11/2001

      Contains routines responsible for the deallocation of a data
      structure used to hold an attribute-value paired list.

      ROUTINES:
#cat: freefet - deallocates the memory for an fet structure.
#cat:

***********************************************************************/

#include <fet.h>
#include <stdlib.h>

void freefet(FET *fet)
{
  int item;
  for (item=0;item<fet->num;item++){
      free (fet->names[item]);
      free (fet->values[item]);
  }
  free((char *)fet->names);
  free((char *)fet->values);
  free(fet);
}
