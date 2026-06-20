#!/usr/bin/env bash
set -u

pass=0
fail=0

run_case() {
  local file="$1"
  local expected="$2"
  echo "==> $file [$expected]"

  if java SemanticParser "$file" > /tmp/t3_comp_stdout.txt 2> /tmp/t3_comp_stderr.txt; then
    status=0
  else
    status=$?
  fi

  if [[ "$expected" == "ok" && "$status" -eq 0 ]]; then
    echo "PASS"
    pass=$((pass + 1))
  elif [[ "$expected" == "erro" && "$status" -ne 0 ]]; then
    echo "PASS"
    pass=$((pass + 1))
  else
    echo "FAIL"
    echo "--- stdout ---"
    cat /tmp/t3_comp_stdout.txt
    echo "--- stderr ---"
    cat /tmp/t3_comp_stderr.txt
    fail=$((fail + 1))
  fi
  echo
}

run_case testes/01-correto-basico.mjava ok
run_case testes/02-correto-polimorfismo.mjava ok
run_case testes/03-erro-classe-nao-declarada.mjava erro
run_case testes/04-erro-variavel-nao-declarada.mjava erro
run_case testes/05-erro-tipo-atribuicao.mjava erro
run_case testes/06-erro-if-nao-boolean.mjava erro
run_case testes/07-erro-metodo-inexistente.mjava erro
run_case testes/08-erro-parametros.mjava erro
run_case testes/09-erro-polimorfismo-invalido.mjava erro
run_case testes/10-erro-array-nao-suportado.mjava erro

echo "Resumo: $pass passou/passaram, $fail falhou/falharam."
[[ "$fail" -eq 0 ]]
