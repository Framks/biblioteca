% Questao 2: operacoes recursivas, sem importar ou usar o modulo lists.
% Modos esperados: adiciona_fim(?X, +L1, ?L2), inverte_lista(+L1, ?L2)
% e ultimo(?X, +L), com listas de entrada fechadas e finitas.
% remove_todos(+X, +L1, ?L2) e conta_ocorrencias(+X, +L, ?N) esperam
% X e elementos sem variaveis; == compara termos sem fazer unificacao.

adiciona_fim(X, [], [X]).
adiciona_fim(X, [Cabeca | Cauda], [Cabeca | Resultado]) :-
    adiciona_fim(X, Cauda, Resultado).

remove_todos(_, [], []).
remove_todos(X, [Cabeca | Cauda], Resultado) :-
    (   X == Cabeca
    ->  remove_todos(X, Cauda, Resultado)
    ;   Resultado = [Cabeca | Restantes],
        remove_todos(X, Cauda, Restantes)
    ).

inverte_lista(Lista, Invertida) :-
    inverte_com_acumulador(Lista, [], Invertida).

inverte_com_acumulador([], Acumulador, Acumulador).
inverte_com_acumulador([Cabeca | Cauda], Acumulador, Invertida) :-
    inverte_com_acumulador(Cauda, [Cabeca | Acumulador], Invertida).

conta_ocorrencias(_, [], 0).
conta_ocorrencias(X, [Cabeca | Cauda], N) :-
    conta_ocorrencias(X, Cauda, Restantes),
    (X == Cabeca -> N is Restantes + 1 ; N = Restantes).

ultimo(X, [X]).
ultimo(X, [_ | Cauda]) :-
    ultimo(X, Cauda).
