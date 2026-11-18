% Questao 1: grafo nao orientado de estacoes de metro.
% Cada aresta e declarada uma unica vez; estacao6 e estacao7 sao isoladas.

estacao(estacao1).
estacao(estacao2).
estacao(estacao3).
estacao(estacao4).
estacao(estacao5).
estacao(estacao6).
estacao(estacao7).

aresta(estacao1, estacao2).
aresta(estacao2, estacao3).
aresta(estacao3, estacao1).
aresta(estacao3, estacao4).
aresta(estacao4, estacao5).

% Os extremos sao enumerados antes do once, mantendo consultas com variaveis.
ligada(A, B) :-
    estacao(A),
    estacao(B),
    dif(A, B),
    once((aresta(A, B) ; aresta(B, A))).

grau(Estacao, Grau) :-
    estacao(Estacao),
    findall(Vizinha, ligada(Estacao, Vizinha), Vizinhas),
    length(Vizinhas, Grau).

% Estacoes livre recebe todas as declaradas. Uma lista fornecida deve ser
% fechada e conter apenas estacoes declaradas, sem variaveis.
% O grau e calculado no grafo completo; empates saem por retrocesso.
menor_vizinhanca(Estacoes, X) :-
    candidatas(Estacoes, Candidatas),
    pertence(X, Candidatas),
    grau(X, Grau),
    \+ (pertence(Outra, Candidatas), grau(Outra, OutroGrau), OutroGrau < Grau).

candidatas(Estacoes, Candidatas) :-
    (var(Estacoes) -> findall(E, estacao(E), Estacoes) ; true),
    is_list(Estacoes),
    ground(Estacoes),
    sort(Estacoes, Candidatas),
    todas_declaradas(Candidatas).

todas_declaradas([]).
todas_declaradas([Estacao | Restantes]) :-
    estacao(Estacao),
    todas_declaradas(Restantes).

pertence(X, [X | _]).
pertence(X, [_ | Restantes]) :-
    pertence(X, Restantes).

% Admite caminho de comprimento zero de uma estacao declarada para si mesma.
% A lista de visitadas impede ciclos; once evita repetir o mesmo par A-B.
existe_caminho(A, B) :-
    estacao(A),
    estacao(B),
    once(caminho(A, B, [A])).

caminho(A, A, _).
caminho(A, B, Visitadas) :-
    ligada(A, Proxima),
    \+ pertence(Proxima, Visitadas),
    caminho(Proxima, B, [Proxima | Visitadas]).
