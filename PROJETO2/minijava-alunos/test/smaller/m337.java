// testando this (erro)
class m337
{
   public static void main(String[] args)
   {
      System.out.println(new a().i());
   }
}

class a
{
   public a A(){return this.A();}
   public int i(){ return 0; }
}