/***********************************************************************
      LIBRARY: FET - Feature File/List Utilities

      FILE:    EXTRFET.C
      AUTHOR:  Michael Garris
      DATE:    01/11/2001

      Contains routines responsible for locating and returning the
      value stored with a specified attribute in an attribute-value
      paired list.

      ROUTINES:
#cat: extractfet - returns the specified feature entry from an fet structure.
#cat:              Exits on error.
#cat: extractfet_ret - returns the specified feature entry from an fet
#cat:              structure.  Returns on error.

***********************************************************************/

#include <stdio.h>
#include <string.h>
#include <fet.h>
#include "util.h"

/*******************************************************************/
char *extractfet(char *feature, FET *fet)
{
  int item;
  char *value;

  for (item = 0;
       (item < fet->num) && (strcmp(fet->names[item],feature) != 0);
       item++);
  if (item>=fet->num)
     fatalerr("extractfet",feature,"not found");
  if(fet->values[item] != (char *)NULL){
      value = (char *)strdup(fet->values[item]);
      if (value == (char *)NULL)
         syserr("extractfet","strdup","value");
  }
  else
      value = (char *)NULL;
  return(value);
}

/*******************************************************************/
int extractfet_ret(char **ovalue, char *feature, FET *fet)
{
  int item;
  char *value;

  for (item = 0;
       (item < fet->num) && (strcmp(fet->names[item],feature) != 0);
       item++);
  if (item>=fet->num){
     fprintf(stderr, "ERROR : extractfet_ret : feature %s not found\n",
             feature);
     return(-2);
  }
  if(fet->values[item] != (char *)NULL){
      value = (char *)strdup(fet->values[item]);
      if (value == (char *)NULL){
         fprintf(stderr, "ERROR : extractfet_ret : strdup : value\n");
         return(-3);
     }
  }
  else
      value = (char *)NULL;

  *ovalue = value;

  return(0);
}
