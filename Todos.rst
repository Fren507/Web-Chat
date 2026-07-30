Todos
=====

This file lists project-level TODOs and session tasks.

Current session TODOs
---------------------
No pending todos were found in the session database when this file was generated.

How to use
----------
- Add items below using reStructuredText list syntax.
- Prefer short gerund-style titles and include a brief description and status.

Example
-------
- Implement user auth (status: pending)
  Implement JWT-based login, refresh tokens, and bcrypt password hashing.

Managing todos programmatically
-------------------------------
The session database exposed to this environment has a `todos` table with columns:
`id, title, description, status, created_at, updated_at`.
Use the session SQL interface to insert or update todos, for example:

INSERT INTO todos (id, title, description) \
VALUES ('user-auth', 'Implement user auth', 'Add JWT login and refresh endpoints');

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
