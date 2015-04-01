// chamada de metodo virtual(OK)
class m308
{
    public static void main(String[] args)
    {
        System.out.println(10);
    }
}

class a extends c
{
    public int a(){return this.i() + 1;}
}

class b extends c
{
    public int a(){return this.i() + 3;}
}

class c extends d
{
}

class d extends m308
{
    public int i(){return 0;}
}
