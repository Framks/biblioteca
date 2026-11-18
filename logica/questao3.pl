% Questao 3: familia Almeida. progenitor(P, F) significa que P e pai ou mae de F.

progenitor(antonio, rafael).
progenitor(antonio, juliana).
progenitor(beatriz, rafael).
progenitor(beatriz, juliana).
progenitor(rafael, felipe).
progenitor(rafael, clara).
progenitor(camila, felipe).
progenitor(camila, clara).
progenitor(juliana, helena).
progenitor(marcos, helena).
progenitor(felipe, miguel).
progenitor(helena, miguel).

masculino(antonio).
masculino(rafael).
masculino(felipe).
masculino(marcos).
masculino(miguel).

feminino(beatriz).
feminino(juliana).
feminino(camila).
feminino(clara).
feminino(helena).

pessoa(X) :- masculino(X).
pessoa(X) :- feminino(X).

% Os pares sao enumerados antes do once. Assim, progenitores compartilhados
% nao repetem respostas, mesmo quando os dois argumentos estao livres.
irmaos(X, Y) :-
    pessoa(X),
    pessoa(Y),
    dif(X, Y),
    once((progenitor(P, X), progenitor(P, Y))).

irmao(X, Y) :- masculino(X), irmaos(X, Y).
irma(X, Y) :- feminino(X), irmaos(X, Y).

pai(X, Y) :- masculino(X), progenitor(X, Y).
mae(X, Y) :- feminino(X), progenitor(X, Y).

% avou e o avo masculino; avo e a avo feminina, conforme o enunciado.
avou(X, Y) :-
    masculino(X),
    pessoa(Y),
    once((progenitor(X, P), progenitor(P, Y))).

avo(X, Y) :-
    feminino(X),
    pessoa(Y),
    once((progenitor(X, P), progenitor(P, Y))).

% Tios e tias apenas por consanguinidade: irmaos de um progenitor.
tio(X, Y) :-
    masculino(X),
    pessoa(Y),
    once((progenitor(P, Y), irmaos(X, P))).

tia(X, Y) :-
    feminino(X),
    pessoa(Y),
    once((progenitor(P, Y), irmaos(X, P))).

primos(X, Y) :-
    pessoa(X),
    pessoa(Y),
    dif(X, Y),
    once((progenitor(PX, X), progenitor(PY, Y), irmaos(PX, PY))).

primo(X, Y) :- masculino(X), primos(X, Y).
prima(X, Y) :- feminino(X), primos(X, Y).

% X descende de Y: Y e ancestral de X. A base familiar e aciclica.
% once elimina provas repetidas por ramos distintos, sem limitar geracoes.
descendente(X, Y) :-
    pessoa(X),
    pessoa(Y),
    once(ancestral(Y, X)).

ancestral(X, Y) :- progenitor(X, Y).
ancestral(X, Y) :- progenitor(X, Intermediario), ancestral(Intermediario, Y).
