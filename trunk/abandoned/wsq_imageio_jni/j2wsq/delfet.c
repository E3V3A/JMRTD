/***********************************************************************
      LIBRARY: FET - Feature File/List Utilities

      FILE:    DELFET.C
      AUTHOR:  Michael Garris
      DATE:    01/11/2001

      Contains routines responsible for removing an entry from an
      attribute-value paired list.

      ROUTINES:
#cat: deletefet - removes the specified feature entry from an fet structure.
#cat:             Exits on error.
#cat: deletefet_ret - removes the specified feature entry from an fet
#cat:             structure.  Returns on error.

***********************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
// #include <util.h>
#include <fet.h>

/*********************************************************************/
void deletefet(char *feature, FET *fet)
{
  int item;

  for (item = 0; 
       (item < fet->num) && (strcmp(fet->names[item],feature) != 0);
       item++);
  if(item >= fet->num)
     fatalerr("deletefet",feature,"Feature not found");
  free(fet->names[item]);
  if(fet->values[item] != (char *)NULL)
     free(fet->values[item]);
  for (++item;item<fet->num;item++){
      fet->names[item-1] = fet->names[item];
      fet->values[item-1] = fet->values[item];
  }
  fet->names[fet->num-1] = '\0';
  fet->values[fet->num-1] = '\0';
  (fet->num)--;
}

/*********************************************************************/
int deletefet_ret(char *feature, FET *fet)
{
  int item;

  for (item = 0; 
       (item < fet->num) && (strcmp(fet->names[item],feature) != 0);
       item++);
  if(item >= fet->num){
    fprintf(stderr, "ERROR : deletefet_ret : feature %s not found\n",
            feature);
     return(-2);
  }
  free(fet->names[item]);
  if(fet->values[item] != (char *)NULL)
     free(fet->values[item]);
  for (++item;item<fet->num;item++){
      fet->names[item-1] = fet->names[item];
      fet->values[item-1] = fet->values[item];
  }
  fet->names[fet->num-1] = '\0';
  fet->values[fet->num-1] = '\0';
  (fet->num)--;

  return(0);
}
