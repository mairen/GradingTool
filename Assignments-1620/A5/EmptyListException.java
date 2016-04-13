public class EmptyListException extends RuntimeException
{
       public EmptyListException ()
       {
               this ("List");
       }
       // constructor with .String. parameter
       public EmptyListException (String name) {
               // call super class constructor
               super (name + " is empty");
       }
}