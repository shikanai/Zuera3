// metodo com o mesmo nome da classe
class m111
{
   public static void main(String[] args)
   {
      System.out.println(new a().i());
   }
}

class a
{
   a a;
   public a a(){
	   System.out.println(1);
	   a = new a(); 
	   return this;
   }
   
   public int i(){ 
	   return 0; 
   }
}
