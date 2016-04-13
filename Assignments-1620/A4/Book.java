public class Book implements hasKey
{
       private String title;
       private String author;
	   private int ISBN;

       public Book()
       {
               ISBN = 0;
               title = "";
               author = "";
       }

       /* I moved "int i" from the last position to the first one for F15. */
       public Book(int i, String t, String a)
       {
               ISBN = i;
               title = t;
               author = a;
       }

       public String getTitle()
       {
               return title;
       }

       public String getAuthor()
       {
               return author;
       }

       public boolean equals(Object b)
       {
               if(this.ISBN == ((Book)b).ISBN)
                       return true;
               else
                       return false;
       }

       public String toString()
       {
               return String.format("%s\n%s\n%d", title, author, ISBN);
       }

	  public int getKey()
	  {
			return ISBN;
	  }
}

