/***********************************************************************
      LIBRARY: FET - Feature File/List Utilities

      FILE:    LKUPFET.C
      AUTHOR:  Michael Garris
      DATE:    01/11/2001

      Contains routines responsible for looking up the value of
      a specified attribute in and attribute-value paired list.

      ROUTINES:
#cat: lookupfet - returns the specified feature entry from an fet
#cat:             structure.  Returns TRUE if found, FALSE if not.

***********************************************************************/

#include <stdio.h>
#include <string.h>
#include <fet.h>
#include <defs.h>

/*******************************************************************/
int lookupfet(char **ovalue, char *feature, FET *fet)
{
  int item;
  char *value;

  for (item = 0;
       (item < fet->num) && (strcmp(fet->names[item],feature) != 0);
       item++);
  if (item>=fet->num){
     return(FALSE);
  }
  if(fet->values[item] != (char *)NULL){
      value = (char *)strdup(fet->values[item]);
      if (value == (char *)NULL){
         fprintf(stderr, "ERROR : lookupfet : strdup : value\n");
         return(-2);
     }
  }
  else
      value = (char *)NULL;

  *ovalue = value;

  return(TRUE);
}
