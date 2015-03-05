/***********************************************************************
      LIBRARY: UTIL - General Purpose Utility Routines

      FILE:    SYSERR.C
      AUTHOR:  Michael Garris
      DATE:    12/19/1990

      Contains routines responsible for exiting upon an system error.

      ROUTINES:
#cat: syserr - exits on error with a status of -1, printing to stderr a
#cat:          caller-defined message.

***********************************************************************/

/* LINTLIBRARY */

#include <stdio.h>
#include <stdlib.h>
#include "util.h"


void syserr( char *funcname, char *syscall, char *msg)
{

   (void) fflush(stdout);
   if(msg == NULL)
      (void) fprintf(stderr,"ERROR: %s: %s\n",funcname,syscall);
   else
      (void) fprintf(stderr,"ERROR: %s: %s: %s\n",funcname,syscall,msg);
   (void) fflush(stderr);

   exit(-1);
}
