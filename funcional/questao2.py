"""Questão 2: valores que aparecem exatamente uma vez, na ordem original."""

from itertools import groupby


def filtrar_na_ordem(numeros, unicos):
    """Selecione os valores únicos percorrendo a sequência original."""
    return list(filter(lambda numero: numero in unicos, numeros))


def valores_unicos(numeros):
    """Agrupe uma cópia ordenada e guarde as chaves dos grupos unitários."""
    return filtrar_na_ordem(
        numeros,
        frozenset(
            map(
                lambda grupo: grupo[0],
                filter(
                    lambda grupo: len(tuple(grupo[1])) == 1,
                    groupby(sorted(numeros)),
                ),
            )
        ),
    )


if __name__ == "__main__":
    print(" ".join(map(str, valores_unicos(tuple(map(int, input().split()))))))
