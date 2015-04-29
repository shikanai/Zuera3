/*****************************************************
Esta classe Codegen é a responsável por emitir LLVM-IR. 
Ela possui o mesmo método 'visit' sobrecarregado de
acordo com o tipo do parâmetro. Se o parâmentro for
do tipo 'While', o 'visit' emitirá código LLVM-IR que 
representa este comportamento. 
Alguns métodos 'visit' já estão prontos e, por isso,
a compilação do código abaixo já é possível.

class a{
    public static void main(String[] args){
    	System.out.println(1+2);
    }
}

O pacote 'llvmast' possui estruturas simples 
que auxiliam a geração de código em LLVM-IR. Quase todas 
as classes estão prontas; apenas as seguintes precisam ser 
implementadas: 

// llvmasm/LlvmBranch.java
// llvmasm/LlvmIcmp.java
// llvmasm/LlvmMinus.java
// llvmasm/LlvmTimes.java


Todas as assinaturas de métodos e construtores 
necessárias já estão lá. 


Observem todos os métodos e classes já implementados
e o manual do LLVM-IR (http://llvm.org/docs/LangRef.html) 
como guia no desenvolvimento deste projeto. 

****************************************************/
package llvm;

import semant.Env;
import syntaxtree.*;
import llvmast.*;

import java.util.*;

public class Codegen extends VisitorAdapter{
	private List<LlvmInstruction> assembler;
	private Codegen codeGenerator;

  	private SymTab symTab;
	private ClassNode classEnv; 	// Aponta para a classe atualmente em uso em symTab
	private MethodNode methodEnv; 	// Aponta para a metodo atualmente em uso em symTab

	public static int counter_label = 0;

	public Codegen(){
		assembler = new LinkedList<LlvmInstruction>();
	}

	// Método de entrada do Codegen
	public String translate(Program p, Env env){	
		codeGenerator = new Codegen();
		
		// Preenchendo a Tabela de Símbolos
		// Quem quiser usar 'env', apenas comente essa linha
		// codeGenerator.symTab.FillTabSymbol(p);
		
		// Formato da String para o System.out.printlnijava "%d\n"
		codeGenerator.assembler.add(new LlvmConstantDeclaration("@.formatting.string", "private constant [4 x i8] c\"%d\\0A\\00\""));	

		// NOTA: sempre que X.accept(Y), então Y.visit(X);
		// NOTA: Logo, o comando abaixo irá chamar codeGenerator.visit(Program), linha 75
		p.accept(codeGenerator);

		// Link do printf
		List<LlvmType> pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		pts.add(LlvmPrimitiveType.DOTDOTDOT);
		codeGenerator.assembler.add(new LlvmExternalDeclaration("@printf", LlvmPrimitiveType.I32, pts)); 
		List<LlvmType> mallocpts = new LinkedList<LlvmType>();
		mallocpts.add(LlvmPrimitiveType.I32);
		codeGenerator.assembler.add(new LlvmExternalDeclaration("@malloc", new LlvmPointer(LlvmPrimitiveType.I8),mallocpts)); 


		String r = new String();
		for(LlvmInstruction instr : codeGenerator.assembler)
			r += instr+"\n";
		return r;
	}

	public LlvmValue visit(Program n){
		n.mainClass.accept(this);

		for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
			c.head.accept(this);

		return null;
	}

	public LlvmValue visit(MainClass n){
		
		// definicao do main 
		assembler.add(new LlvmDefine("@main", LlvmPrimitiveType.I32, new LinkedList<LlvmValue>()));
		assembler.add(new LlvmLabel(new LlvmLabelValue("entry")));
		LlvmRegister R1 = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		assembler.add(new LlvmAlloca(R1, LlvmPrimitiveType.I32, new LinkedList<LlvmValue>()));
		assembler.add(new LlvmStore(new LlvmIntegerLiteral(0), R1));

		// Statement é uma classe abstrata
		// Portanto, o accept chamado é da classe que implementa Statement, por exemplo,  a classe "Print". 
		n.stm.accept(this);  

		// Final do Main
		LlvmRegister R2 = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmLoad(R2,R1));
		assembler.add(new LlvmRet(R2));
		assembler.add(new LlvmCloseDefinition());
		return null;
	}
	
	public LlvmValue visit(Plus n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmPlus(lhs,LlvmPrimitiveType.I32,v1,v2));
		return lhs;
	}
	
	public LlvmValue visit(Print n){

		LlvmValue v =  n.exp.accept(this);

		// getelementptr:
		LlvmRegister lhs = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I8));
		LlvmRegister src = new LlvmNamedValue("@.formatting.string",new LlvmPointer(new LlvmArray(4,LlvmPrimitiveType.I8)));
		List<LlvmValue> offsets = new LinkedList<LlvmValue>();
		offsets.add(new LlvmIntegerLiteral(0));
		offsets.add(new LlvmIntegerLiteral(0));
		List<LlvmType> pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		List<LlvmValue> args = new LinkedList<LlvmValue>();
		args.add(lhs);
		args.add(v);
		assembler.add(new LlvmGetElementPointer(lhs,src,offsets));

		pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		pts.add(LlvmPrimitiveType.DOTDOTDOT);
		
		// printf:
		assembler.add(new LlvmCall(new LlvmRegister(LlvmPrimitiveType.I32),
				LlvmPrimitiveType.I32,
				pts,				 
				"@printf",
				args
				));
		return null;
	}
	
	public LlvmValue visit(IntegerLiteral n){
		return new LlvmIntegerLiteral(n.value);
	};
	
	// Todos os visit's que devem ser implementados	
	public LlvmValue visit(ClassDeclSimple n){
		
		//seguindo o padrao do slide 21/47 parte 1 llvm...
		
		System.out.format("classdeclsimple :)\n");
		
		int i, j;
		
		//criando uma string, onde sera colocado o assembly referente ao type. (type {...})
		StringBuilder classTypes = new StringBuilder();
		
		//criando uma string onde sera colocado o nome da classe
		StringBuilder className = new StringBuilder();
		
		//lista ligada onde serao armazenados os tipos das variaveis
		List<LlvmType> typeList = new LinkedList<LlvmType>();
		
		//%class.name
		className.append("%class.");
		className.append(n.name.s);
		
		//type { type1, type2, ... } - vamos utilizar LlvmStructure
		classTypes.append("type ");				
		
		if (n.varList != null) {
			j = n.varList.size();
			System.out.format("Numero de variaveis: %d\n", j);
			System.out.format("tipos das variaveis:\n");
			for (i = 0; i < j; i++){
				//itera a lista de variaveis para pegar todos os tipos e appendar em classTypes.
				LlvmValue variable_type = n.varList.head.type.accept(this);
				
				System.out.format("%s ", variable_type);
				
				//adiciona os tipos de variaveis
				typeList.add((LlvmType) variable_type);
				
				n.varList = n.varList.tail;
			}
		}

		//Structure onde serao colocados os tipos, formatados pelo LlvmStructure
		LlvmStructure listaDeTipos = new LlvmStructure(typeList);
		
		System.out.format("\nListaDetipos: %s\n", listaDeTipos.toString());
		
		//appenda a lista de tipos no classTypes
		classTypes.append(listaDeTipos.toString());
		
		System.out.format("\nclassType final: %s\n", classTypes);
		
		System.out.format("className: %s\n",className);
		
		// Adiciona declaracao de classe no assembly
		assembler.add(new LlvmConstantDeclaration(className.toString(),classTypes.toString()));
		
		if(n.methodList != null) {
			j = n.methodList.size();
			//itera todos os metodos da classe
			for (i = 0; i < j; i++) {
				MethodDecl method = n.methodList.head;
				
				System.out.format("method: %s ", method);
				
				//passamos pelo visit do method, para gerar o assembly do metodo.
				visit(method);	
				
				n.methodList = n.methodList.tail;
			}
		}
		
		return null;
		
	}
	
	//provavelmente nao vamos ter tempo de fazer esse...(espero que tenhamos :D)
	public LlvmValue visit(ClassDeclExtends n){
		System.out.format("classdeclextends :)\n");
		return null;
		
	}
	
	//declaracao de variavel.
	public LlvmValue visit(VarDecl n){
		System.out.format("vardecl :)\n");
		
		LlvmType varType = (LlvmType) n.type.accept(this);
		
		StringBuilder varDeclaration = new StringBuilder();
		
		//criando o nome do endereco onde sera alocada a variavel
		varDeclaration.append("%");
		varDeclaration.append(n.name.s);
		varDeclaration.append("_address");
		
		//varType *
		LlvmPointer varTypePtr = new LlvmPointer(varType);
		
		//%name_address
		LlvmRegister registerVar = new LlvmRegister(varDeclaration.toString(), varTypePtr);
		
		//gera o assembly: %name_address = alloca type
		assembler.add(new LlvmAlloca(registerVar, varType, new LinkedList<LlvmValue>()));
		
		return registerVar;
		
	}
	
	//rv
	//para as declaracoes de metodos, vamos simplesmente escrever a codificacao no .s
	public LlvmValue visit(MethodDecl n){
		
		System.out.format("methoddecl :)\n");
		
		int i,j;
		
		LinkedList<LlvmValue> parametros = new LinkedList<LlvmValue>();
		LlvmType retType = (LlvmType) n.returnType.accept(this);
		StringBuilder declString = new StringBuilder();
		
		System.out.format("body.head: %s\n",n.body.head);
		System.out.format("locals: %s\n",n.locals.head);
		System.out.format("name: %s\n",n.name);
		System.out.format("return type: %s\n",n.returnType);
		System.out.format("return exp: %s\n",n.returnExp);
		
		//preenchendo a lista de parametros do metodo
		if(n.formals != null) {
			System.out.format("formals: %s\n",n.formals.head);
			j = n.formals.size();
			System.out.format("formals size: %d\n", j);
			
			//itera todos os parametros do metodo
			for (i = 0; i < j; i++) {
				
				LlvmValue param = n.formals.head.accept(this);
				
				System.out.format("formals: %s \n", n.formals.head);
				parametros.add(param);
				n.formals = n.formals.tail;
				
			}
		}
		
		declString.append("@__");
		declString.append(n.name);
		
		System.out.format("declString: %s\n",declString);
		System.out.format("retType: %s\n",retType);
		
		// Adiciona define de classe no assembly
		assembler.add(new LlvmDefine(declString.toString(), retType, parametros));
		
		//Apos o define, devemos comecar a implementacao do metodo...
		
		//copiando da main... Criando entrypoint
		assembler.add(new LlvmLabel(new LlvmLabelValue("entryMethod")));
		
		j = parametros.size();
		
		//alocando memoria para todos os parametros
		for(i = 0; i < j ; i++){
			
			LlvmValue parametro_atual = parametros.get(i);
			
			System.out.format("parametro_atual: %s\n",parametro_atual);
			
			//cria ponteiro para o tipo, que sera utilizado como endereco de alocacao.
			LlvmPointer pointer_type = new LlvmPointer(parametro_atual.type);
			
			StringBuilder addr_name = new StringBuilder();
			
			addr_name.append(parametro_atual);
			addr_name.append("_address");
			
			//%name_address
			LlvmValue addr = new LlvmNamedValue(addr_name.toString(), pointer_type);
			
			//aloca memoria para endereco
			assembler.add(new LlvmAlloca(addr, parametro_atual.type, new LinkedList<LlvmValue>()));
			
			//armazena valor no endereco apontado por addr
			assembler.add(new LlvmStore(parametro_atual,addr));
		}
		
		System.out.format("locals size: %s\n",n.locals.size());
		
		//itera todos os locals
		
		j = n.locals.size();
		for(i=0;i<j;i++){
			
			//chama varDecl para cada variavel local
			n.locals.head.accept(this);
			
			System.out.format("locals****: %s \n", n.locals.head);
			
			n.locals = n.locals.tail;
			
		}
		
		//itera nos statements do metodo
		
		j = n.body.size();
		for(i=0;i<j;i++){
				
			System.out.format("body****: %s \n", n.body.head);
			
			//gera codigo para cada stmt seguinte.
			n.body.head.accept(this);
					
			n.body = n.body.tail;
					
		}

		//retorno...
		assembler.add(new LlvmRet(n.returnExp.accept(this)));
		
		//}
		assembler.add(new LlvmCloseDefinition());
		
		return null;
		
	}
	
	//variavel de parametro...
	public LlvmValue visit(Formal n){
		System.out.format("formal :)\n");
		
		StringBuilder name = new StringBuilder();
		
		name.append("%");
		name.append(n.name.s);
		
		//variable -> %name, com tipo n.type
		LlvmNamedValue variable = new LlvmNamedValue(name.toString(), (LlvmType)n.type.accept(this));
		
		return variable;		
	}
	public LlvmValue visit(IntArrayType n){
		System.out.format("intarraytype :)\n");
		
		//retorna novo tipo criado... Verificar se da certo haha :P
		return LlvmPrimitiveType.I32PTR;
		
	}
	//test
	//Como nao podemos retornar um LlvmType, extendemos LlvmType para LlvmValue
	public LlvmValue visit(BooleanType n){
		return LlvmPrimitiveType.I1;
	}
	//test
	//Como nao podemos retornar um LlvmType, extendemos LlvmType para LlvmValue
	public LlvmValue visit(IntegerType n){
		return LlvmPrimitiveType.I32;
	}
	public LlvmValue visit(IdentifierType n){
		System.out.format("identifiertype :)\n");
		return null;
		
	}
	
	//na implementacao do block ,simplesmente iteramos o body inteiro do block
	public LlvmValue visit(Block n){
		System.out.format("block :)\n");
		
		//verifica se existe algum elemento no block
		if(n.body != null){
			int i,j;
			
			j = n.body.size();
			
			//itera em todos os elementos
			for(i=0;i<j;i++){
			
				System.out.format("@block body: %s\n", n.body.head);
				
				//gera codigo de cada parte do block
				n.body.head.accept(this);
						
				n.body = n.body.tail;
				
				
			}
		}
		
		return null;
		
	}
	public LlvmValue visit(If n){
		//pega o registrador referente a condicao do if em cond
		LlvmValue cond = n.condition.accept(this);
		System.out.format("cond: %s\n",cond);
		System.out.format("n.condition: %s\n", n.condition);
		
		//cria as labels referentes a cada branch
		LlvmLabelValue brTrue = new LlvmLabelValue("iflabeltrue"+counter_label);
		counter_label++;
		System.out.format("label1: %s\n",brTrue);
		LlvmLabelValue brFalse = new LlvmLabelValue("iflabelfalse"+counter_label);
		counter_label++;
		System.out.format("label2: %s\n",brFalse);
		//cria label referente ao break da label para a qual pulamos.
		LlvmLabelValue brBreak = new LlvmLabelValue("iflabelbreak"+counter_label);
		counter_label++;
		System.out.format("label3: %s\n",brBreak);
		
		if(n.elseClause!=null && n.thenClause!=null){
			//faz o branch condicional, pulando para label brTrue se a cond retornar 1, e para brFalse se retornar 0
			assembler.add(new LlvmBranch(cond, brTrue, brFalse));

			//label brTrue
			assembler.add(new LlvmLabel(brTrue));
			
			//gera codigo contido na thenClause
			n.thenClause.accept(this);

			System.out.format("thenClause: %s\n",n.thenClause);

			//se pulou para label brTrue, agora ele pula para o brBreak, com o intuito de pular o brFalse
			assembler.add(new LlvmBranch(brBreak));
			
			assembler.add(new LlvmLabel(brFalse));
			
			System.out.format("elseClause: %s\n",n.elseClause);

			//gera o codigo contido na elseClause
			n.elseClause.accept(this);
			
			assembler.add(new LlvmBranch(brBreak));
			
		} else if(n.elseClause!=null && n.thenClause==null){
			//faz o branch condicional, pulando para label brBreak se a cond retornar 1, e para brFalse se retornar 0
			assembler.add(new LlvmBranch(cond, brBreak, brFalse));

			System.out.format("elseClause != null");
		
			assembler.add(new LlvmLabel(brFalse));

			System.out.format("elseClause: %s\n",n.elseClause);

			//gera o codigo contido na elseClause
			n.elseClause.accept(this);
			
			assembler.add(new LlvmBranch(brBreak));
		} else {
			//faz o branch condicional, pulando para label brTrue se a cond retornar 1, e para brBreak se retornar 0
			assembler.add(new LlvmBranch(cond, brTrue, brBreak));
			//label brTrue
			assembler.add(new LlvmLabel(brTrue));
			
			//gera codigo contido na thenClause
			n.thenClause.accept(this);

			System.out.format("thenClause: %s\n",n.thenClause);
			
			//se pulou para label brTrue, agora ele pula para o brBreak, com o intuito de pular o brFalse
			assembler.add(new LlvmBranch(brBreak));
		}
		//assembler.add(new LlvmBranch(brBreak));
		//label do break
		assembler.add(new LlvmLabel(brBreak));
		
		return null;
	}
	public LlvmValue visit(While n){
		
		//pega o registrador referente a condicao do while em cond
		LlvmValue cond = n.condition.accept(this);
		System.out.format("cond: %s\n",cond);
		System.out.format("n.condition: %s\n", n.condition);
		
		//cria as labels referentes a cada branch
		
		//cria label referente ao while.
		LlvmLabelValue brTrue = new LlvmLabelValue("whilelabeltrue"+counter_label);
		counter_label++;
		System.out.format("label1: %s\n",brTrue);
		
		//cria label referente ao break do while.
		LlvmLabelValue brBreak = new LlvmLabelValue("whilelabelbreak"+counter_label);
		counter_label++;
		System.out.format("label2: %s\n",brBreak);
				
		//faz o branch condicional, pulando para label brTrue se a cond retornar 1, e para brBreak se retornar 0
		//(ou seja, da break quando nao atende mais a condicao)
		
		assembler.add(new LlvmBranch(cond, brTrue, brBreak));
				
		//label brTrue
		assembler.add(new LlvmLabel(brTrue));
		
		System.out.format("body: %s\n",n.body);
		
		//gera codigo contido dentro do while
		n.body.accept(this);
		
		//depois de executar o codigo dentro do while, faz de novo o branch. 
		assembler.add(new LlvmBranch(cond, brTrue, brBreak));
		
		//label do break
		assembler.add(new LlvmLabel(brBreak));
			
		return null;
	}
	
	//nesta chamada, devemos pegar o address da variavel e dar um store no valor do assign nesse endereco
	public LlvmValue visit(Assign n){
		
		System.out.format("assign :)\n");
		
		LlvmValue var = n.var.accept(this);
		StringBuilder address = new StringBuilder();
		
		System.out.format("var_name: %s\n",var);
		//System.out.format("n.var: %s\n",n.var);
		System.out.format("n string: %s\n", n.toString());
		
		address.append("%");
		address.append(var.toString());
		address.append("_address");
		
		LlvmType var_type = var.type;
		
		//%name_address
		LlvmValue pointer = new LlvmNamedValue(address.toString(), new LlvmPointer(var_type));
		
		//valor que vai ser armazenado no endereco apontado pela variavel
		LlvmValue value_to_store = n.exp.accept(this);
		
		System.out.format("n.exp: %s\n",n.exp);
		
		//gera assembly referente ao store: store type %reg, type * %address;
		assembler.add(new LlvmStore(value_to_store, pointer));
		
		return value_to_store;
		
	}
	
	//ToDo
	public LlvmValue visit(ArrayAssign n){
		System.out.format("assign array :)\n");
		return null;
	}

	public LlvmValue visit(And n){
		
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);

		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		LlvmRegister aux1 = new LlvmRegister(LlvmPrimitiveType.I1);
		
		//para termos certeza de que soh sera executado o codigo quando tivermos true e true, vamos usar o LlvmTimes
		//para multiplicar um pelo outro. Assim, quando ao menos um deles for false, teremos resposta zero
		
		//o tipo de v1 e v2 eh o mesmo, entao passamos o de v1
		assembler.add(new LlvmTimes(aux1, v1.type, v1, v2));
		
		LlvmIntegerLiteral aux2 = new LlvmIntegerLiteral(1);
		
		//compara o resultado da multiplicacao com 1(pois todas as vezes que um for false, o resultado de mul vai ser zero).
		assembler.add(new LlvmIcmp(lhs,1,aux1.type,aux1,aux2));
		
		//retorna o registrador contendo o resultado
		return lhs;
	}

	public LlvmValue visit(LessThan n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		//utilizando 0 para lessthan
		
		System.out.format("less than: lhs e rhs: %s %s\n",n.lhs,n.rhs);
		
		//comparamos dois elementos i32, e retornamos um bool.
		assembler.add(new LlvmIcmp(lhs,0,v1.type,v1,v2));
		return lhs;
	}
	//ok
	public LlvmValue visit(Equal n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		//utilizando 1 para equal
		assembler.add(new LlvmIcmp(lhs,1,v1.type,v1,v2));
		return lhs;
	}
	//ok
	public LlvmValue visit(Minus n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmMinus(lhs,LlvmPrimitiveType.I32,v1,v2));
		return lhs;
	}
	//ok
	public LlvmValue visit(Times n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmTimes(lhs,LlvmPrimitiveType.I32,v1,v2));
		return lhs;  
	}
	
	public LlvmValue visit(ArrayLookup n){
		System.out.format("arraylookup :)\n");
		return null;
		
	}
	public LlvmValue visit(ArrayLength n){
		System.out.format("arraylength :)\n");
		return null;
		
	}
	public LlvmValue visit(Call n){
		
		System.out.format("call :)\n");

		LlvmType type = (LlvmType) n.type.accept(this);
		
		LlvmRegister register = new LlvmRegister(type);
		
		System.out.format("type:%s\n", n.type);
		
		//lista dos tipos dos parametros da funcao sendo chamada
		List<LlvmType> types_list = new ArrayList<LlvmType>();
		
		//lista dos parametros da funcao sendo chamada
		List<LlvmValue> parametros_func = new ArrayList<LlvmValue>();
		
		int i, j; 
		
		if(n.actuals != null){
			
			System.out.format("size actuals:%s\n", n.actuals.size());
			
			j = n.actuals.size();
			
			//iterando todos os parametros
			for(i = 0; i < j; i++){
				
				//tipo do parametro atual
				LlvmType aux =(LlvmType) n.actuals.head.type.accept(this);
				
				//parametro atual
				LlvmValue aux2 = n.actuals.head.accept(this);
				
				System.out.format("actuals:%s\n", n.actuals.head);
				
				System.out.format("actuals type:%s\n", n.actuals.head.type);
				
				//incrementando as duas listas
				types_list.add(aux);
				parametros_func.add(aux2);
				n.actuals = n.actuals.tail;
			}
		
		}
		
		System.out.format("@call n.method.s:%s\n", n.method.s);
		
		//gerando o assembly do call da funcao, alterando o nome dela para "@__"+nome, pois esse eh o padrao dos metodos que estamos utilizando.
		assembler.add(new LlvmCall(register,type,types_list,"@__"+n.method.s,parametros_func));
		
		return register;
		
	}
	//ok
	public LlvmValue visit(True n){
		//1 -> true
		return new LlvmBool(1);
	}
	//ok
	public LlvmValue visit(False n){
		//0 -> false
		return new LlvmBool(0);
	}
	
	public LlvmValue visit(IdentifierExp n){
		System.out.format("identifierexp :)\n");
		
		System.out.format("n.type: %s\n",n.type);
		System.out.format("n.name: %s\n",n.name);
		System.out.format("n.string: %s\n",n.toString());
		
		
		//aloca registrador para retorno
		LlvmRegister res = new LlvmRegister((LlvmType)n.type.accept(this));
		
		//pega tipo do identificador
		LlvmPrimitiveType identifier_type = (LlvmPrimitiveType) n.type.accept(this);
		
		// type *
		LlvmPointer pointer_to_type = new LlvmPointer (identifier_type);
		
		//%name_address *
		LlvmNamedValue name = new LlvmNamedValue("%" + n.name.s + "_address",pointer_to_type);
		
		//res = load type * %name_address
		assembler.add(new LlvmLoad(res, name));
		
		return res;
		
	}
	//test
	public LlvmValue visit(This n){
		
		System.out.format("This******** :)\n");
		
		LlvmType thisType = (LlvmType) n.type.accept(this);
		LlvmRegister thisReg = new LlvmRegister("%this", thisType);
		
		return thisReg;
	}
	public LlvmValue visit(NewArray n){
		System.out.format("newarray :)\n");
		return null;
		
	}
	public LlvmValue visit(NewObject n){
		System.out.format("newobject :)\n");
		
		return null;
		
	}
	//ok
	public LlvmValue visit(Not n){
		
		//ideia era um xor com 1, porque ele inverte o bit. 1 xor 1 = 0 e 1 xor 0 = 1
		//Para nao implementar xor usou-se o Icmp com equal e 0 (primeiro termo fixo), 
		//pois 0 == 0 é true (1) e 0 == 1 é false, ou seja, 0.
		LlvmValue v1 = n.exp.accept(this);
        LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
        assembler.add(new LlvmIcmp(lhs,1,v1.type,v1,new LlvmBool(0)));
        return lhs;
        
	}
	public LlvmValue visit(Identifier n){
		System.out.format("identifier :)\n");

		//TODO: descobrir como pegar o tipo do identifier
		LlvmNamedValue identifier = new LlvmNamedValue(n.s, LlvmPrimitiveType.I32);
		
		return identifier;
		
	}
}


/**********************************************************************************/
/* === Tabela de Símbolos ==== 
 * 
 * 
 */
/**********************************************************************************/

class SymTab extends VisitorAdapter{
    public Map<String, ClassNode> classes;     
    private ClassNode classEnv;    //aponta para a classe em uso

    public LlvmValue FillTabSymbol(Program n){
	n.accept(this);
	return null;
}
public LlvmValue visit(Program n){
	n.mainClass.accept(this);

	for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
		c.head.accept(this);

	return null;
}

public LlvmValue visit(MainClass n){
	classes.put(n.className.s, new ClassNode(n.className.s, null, null));
	return null;
}

public LlvmValue visit(ClassDeclSimple n){
	List<LlvmType> typeList = null;
	// Constroi TypeList com os tipos das variáveis da Classe (vai formar a Struct da classe)
	
	List<LlvmValue> varList = null;
	// Constroi VarList com as Variáveis da Classe

	classes.put(n.name.s, new ClassNode(n.name.s, 
										new LlvmStructure(typeList), 
										varList)
      			);
    	// Percorre n.methodList visitando cada método
	return null;
}

	public LlvmValue visit(ClassDeclExtends n){return null;}
	public LlvmValue visit(VarDecl n){return null;}
	public LlvmValue visit(Formal n){return null;}
	public LlvmValue visit(MethodDecl n){return null;}
	public LlvmValue visit(IdentifierType n){return null;}
	public LlvmValue visit(IntArrayType n){return null;}
	public LlvmValue visit(BooleanType n){return null;}
	public LlvmValue visit(IntegerType n){return null;}
}

class ClassNode extends LlvmType {
	ClassNode (String nameClass, LlvmStructure classType, List<LlvmValue> varList){
	}
}

class MethodNode {
}




