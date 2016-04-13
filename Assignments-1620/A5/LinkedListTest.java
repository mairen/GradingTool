import java.util.Scanner;
public class LinkedListTest
{
       public static void main(String[] args)
       {
               LinkedList<Integer> myList = new LinkedList<Integer>("Integer List");

               myList.print();

               System.out.println("Populating from front");

               for(int x = 0; x < 5; x++)
               {
                       myList.insertAtFront(x);
               }

               printLengthAndList(myList);

               System.out.println("Populating from back");

               for(int x = 0; x < 5; x++)
               {
                       myList.insertAtBack(x);
               }

               printLengthAndList(myList);

               System.out.println("Removing from front, then back");

               try
               {
                       myList.removeFromFront();
               }
               catch(EmptyListException ELE)
               {
                       System.out.print(ELE);
               }

               try
               {
                       myList.removeFromBack();
               }
               catch(EmptyListException ELE)
               {
                       System.out.print(ELE);
               }

               printLengthAndList(myList);

               try
               {
                       if(myList.findAndRemove(4))
                       {
                               System.out.println("4 removed");
                       }
                       else
                       {
                               System.out.println("4 not removed");
                       }
               }
               catch(EmptyListException ELE)
               {
                       System.out.println(ELE);
               }

               printLengthAndList(myList);

               try
               {
                       if(myList.findAndRemove(0))
                       {
                               System.out.println("0 removed");
                       }
                       else
                       {
                               System.out.println("0 not removed");
                       }
               }
               catch(EmptyListException ELE)
               {
                       System.out.println(ELE);
               }

               printLengthAndList(myList);

               int position = myList.findItem(5);
               if(position!=-1)
               {
                       System.out.printf("Value of 5 found at %d\n", position);
               }
               else
               {
                       System.out.println("Value of 5 not found");
               }

               position = myList.findItem(0);
               if(position!=-1)
               {
                       System.out.printf("Value of 0 found at %d\n", position);
               }
               else
               {
                       System.out.println("Value of 0 not found");
               }

               System.out.printf("Value at position %d being removed\n", position);
               myList.remove(position);
               printLengthAndList(myList);

               System.out.println("Removing element 0");
               myList.remove(0);
               printLengthAndList(myList);

               System.out.println("Removing from front");
               myList.removeFromFront();
               printLengthAndList(myList);

               System.out.println("Getting value at position 2");
               Integer temp = myList.get(2);
               System.out.printf("Value at postion is %d\n", temp);

               System.out.println("Testing IndexOutOfBoundsException");
               try
               {
                       myList.get(4);
               }
               catch(IndexOutOfBoundsException IOBE)
               {
                       System.out.println(IOBE);
               }

               try
               {
                       myList.get(-1);
               }
               catch(IndexOutOfBoundsException IOBE)
               {
                       System.out.println(IOBE);
               }


               try
               {
                       myList.remove(4);
               }
               catch(IndexOutOfBoundsException IOBE)
               {
                       System.out.println(IOBE);
               }

               try
               {
                       myList.remove(-1);
               }
               catch(IndexOutOfBoundsException IOBE)
               {
                       System.out.println(IOBE);
               }

               System.out.println("End of IOBE test");

               System.out.println("Clearing if not empty");

               if(!myList.isEmpty())
               {
                       myList.clear();
               }

               printLengthAndList(myList);

               System.out.println("Testing EmptyListException");

               try
               {
                       myList.removeFromFront();
               }
               catch(EmptyListException ELE)
               {
                       System.out.println(ELE);
               }

               try
               {
                       myList.removeFromBack();
               }
               catch(EmptyListException ELE)
               {
                       System.out.println(ELE);
               }

               try
               {
                       myList.findAndRemove(0);
               }
               catch(EmptyListException ELE)
               {
                       System.out.println(ELE);
               }

               try
               {
                       myList.remove(0);
               }
               catch(EmptyListException ELE)
               {
                       System.out.println(ELE);
               }

               try
               {
                       myList.get(0);
               }
               catch(EmptyListException ELE)
               {
                       System.out.println(ELE);
               }

               System.out.println("End of EmptyListException test");
       }

       public static void printLengthAndList(LinkedList<Integer> l)
       {
               System.out.printf("Length is: %d\n", l.lengthIs());
               l.print();
       }
}