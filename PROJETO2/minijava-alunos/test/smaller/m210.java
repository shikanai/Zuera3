// heranca entre cinco classes (OK)
class m210
{
    public static void main(String[] args)
    {
    	System.out.println(10);
    }
}

class a extends b
{
   public int i() { return 1; }
}

class b extends c
{
   public int i() { return 0; }
}

class c extends d
{
   public int i() { return 2; }
}

class d extends e
{
   public int i() { return 3; }
}

class e extends m210
{
   public int i() { return 4; }
}