/***********************************************************************
      LIBRARY: FET - Feature File/List Utilities

      FILE:    UPDATFET.C
      AUTHOR:  Michael Garris
      DATE:    01/11/2001

      Contains routines responsible for replacing the value of
      a specified attribute in an attribute-value paird list.

      ROUTINES:
#cat: updatefet - replaces a feature entry in an fet structure, or creates
#cat:             a new entry if the feature does not already exist.
#cat:             Exits on error.
#cat: updatefet_ret - replaces a feature entry in an fet structure, or
#cat:             creates a new entry if the feature does not already exist.
#cat:             Returns on error.

***********************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fet.h>
// #include <util.h>
#include <defs.h>

/***********************************************************************/
void updatefet(char *feature, char *value, FET *fet)
{
  int item;
  int increased, incr;

  for (item = 0;
       (item < fet->num) && (strcmp(fet->names[item],feature) != 0);
       item++);
  if (item < fet->num){
     if(fet->values[item] != (char *)NULL){
        free(fet->values[item]);
        fet->values[item] = (char *)NULL;
     }
     if(value != (char *)NULL){
        fet->values[item] = (char *)strdup(value);
        if(fet->values[item] == (char *)NULL)
           syserr("updatefet","strdup","fet->values[]");
     }
  }
  else{
     if(fet->num >= fet->alloc){
        incr      = fet->alloc / 10;		/* add 10% or 10 which-	*/
        increased = fet->alloc + max(10, incr);	/* ever is larger	*/
        reallocfet(fet, increased);
     }
     fet->names[fet->num] = (char *)strdup(feature);
     if(fet->names[fet->num] == (char *)NULL)
        syserr("updatefet","strdup","fet->names[]");
     if(value != (char *)NULL){
        fet->values[fet->num] = (char *)strdup(value);
        if(fet->values[fet->num] == (char *)NULL)
           syserr("updatefet","strdup","fet->values[]");
     }
     (fet->num)++;
  }
}

/***********************************************************************/
int updatefet_ret(char *feature, char *value, FET *fet)
{
  int ret, item;
  int increased, incr;

  for (item = 0;
       (item < fet->num) && (strcmp(fet->names[item],feature) != 0);
       item++);
  if (item < fet->num){
     if(fet->values[item] != (char *)NULL){
        free(fet->values[item]);
        fet->values[item] = (char *)NULL;
     }
     if(value != (char *)NULL){
        fet->values[item] = (char *)strdup(value);
        if(fet->values[item] == (char *)NULL){
           fprintf(stderr, "ERROR : updatefet_ret : strdup : fet->values[]\n");
           return(-2);
        }
     }
  }
  else{
     if(fet->num >= fet->alloc){
        incr      = fet->alloc / 10;		/* add 10% or 10 which-	*/
        increased = fet->alloc + max(10, incr);	/* ever is larger	*/
	ret = reallocfet_ret(&fet, increased);
        if(ret)
           return(ret);
     }
     fet->names[fet->num] = (char *)strdup(feature);
     if(fet->names[fet->num] == (char *)NULL){
        fprintf(stderr, "ERROR : updatefet_ret : strdup : fet->names[]\n");
        return(-3);
     }
     if(value != (char *)NULL){
        fet->values[fet->num] = (char *)strdup(value);
        if(fet->values[fet->num] == (char *)NULL){
           fprintf(stderr, "ERROR : updatefet_ret : strdup : fet->values[]\n");
           return(-4);
        }
     }
     (fet->num)++;
  }

  return(0);
}
