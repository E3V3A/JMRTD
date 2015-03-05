#ifndef _FET_H
#define _FET_H

#include <stdio.h>

#ifndef True
#define True		1
#define False		0
#endif
#define FET_EXT		"fet"
#define MAXFETS		100
#define MAXFETLENGTH	512

typedef struct fetstruct{
   int alloc;
   int num;
   char **names;
   char **values;
} FET;

/* allocfet.c */
extern FET  *allocfet(int);
extern int  allocfet_ret(FET **, int);
extern FET  *reallocfet(FET *, int);
extern int  reallocfet_ret(FET **, int);
/* delfet.c */
extern void deletefet(char *, FET *);
extern int  deletefet_ret(char *, FET *);
/* extfet.c */
extern char *extractfet(char *, FET *);
extern int extractfet_ret(char **, char *, FET *);
/* freefet.c */
extern void freefet(FET *);
/* lkupfet.c */
extern int lookupfet(char **, char *, FET *);
/* strfet.c */
extern int fet2string(char **, FET *);
extern int string2fet(FET **, char *);
/* updatfet.c */
extern void updatefet(char *, char *, FET *);
extern int  updatefet_ret(char *, char *, FET *);

#endif  /* !_FET_H */
