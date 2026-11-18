"""Questão 3: pares com pelo menos três ocorrências, sem repetições."""

from itertools import groupby


def filtrar_na_ordem(numeros, selecionados):
    """dict.fromkeys fornece cada valor na ordem da primeira ocorrência."""
    return list(filter(lambda numero: numero in selecionados, dict.fromkeys(numeros)))


def pares_frequentes(numeros):
    """Agrupe os pares e selecione as chaves com frequência mínima de três."""
    return filtrar_na_ordem(
        numeros,
        frozenset(
            map(
                lambda grupo: grupo[0],
                filter(
                    lambda grupo: len(tuple(grupo[1])) >= 3,
                    groupby(sorted(filter(lambda numero: numero % 2 == 0, numeros))),
                ),
            )
        ),
    )


if __name__ == "__main__":
    print(" ".join(map(str, pares_frequentes(tuple(map(int, input().split()))))))
