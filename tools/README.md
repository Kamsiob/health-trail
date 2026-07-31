# tools

Scripts the project needs. Each one runs with a single command and no setup beyond Python 3.

```
checks/run_all.py                 every compliance check, reporting all failures
checks/check_copy.py              no em dashes, American English
checks/check_templates.py         the 57 templates against their schema and content rules
checks/check_schema.py            contract/schema.sql against the data contract
checks/check_contract_isolation.py  /contract stays platform neutral
board.py                          read and update the project board
```

`checks/run_all.py` is what continuous integration runs. It prints, on every run, the `TESTING-PERSONAS.md` section 5 checks that are not implemented yet and what each is waiting on, so a green result is never mistaken for broader coverage than it is.

The deterministic fixture generator, issue #17, lands here too.
