"""Seleção e classificação do backfill usuario↔pessoa (funções puras)."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional


MEMBER_AGGREGATORS = {2, 3}
CANDIDATE_AGGREGATOR = 5
ELIGIBLE_AGGREGATORS = MEMBER_AGGREGATORS | {CANDIDATE_AGGREGATOR}

CREATED = "created"
LINKED = "linked"
ALREADY_DONE = "already_done"
SKIPPED = "skipped"


@dataclass(frozen=True)
class PersonRow:
    pessoa_id: int
    nome: str
    email: Optional[str]
    aggregator_id: int


@dataclass(frozen=True)
class UserRow:
    id: int
    email: str
    person_id: Optional[int]
    access_profile: str
    active: bool
    firebase_uid: str


@dataclass(frozen=True)
class Action:
    outcome: str
    pessoa_id: int
    nome: str
    email: Optional[str]
    profile: Optional[str]
    reason: str
    usuario_id: Optional[int] = None


def normalize_email(email: Optional[str]) -> Optional[str]:
    if email is None:
        return None
    trimmed = email.strip().lower()
    return trimmed or None


def profile_for_aggregator(aggregator_id: int) -> Optional[str]:
    if aggregator_id in MEMBER_AGGREGATORS:
        return "MEMBER"
    if aggregator_id == CANDIDATE_AGGREGATOR:
        return "MEMBERSHIP_CANDIDATE"
    return None


def classify(people: list[PersonRow], users: list[UserRow]) -> list[Action]:
    """Classifica cada pessoa elegível com e-mail em created/linked/already_done/skipped."""
    users_by_person = {u.person_id: u for u in users if u.person_id is not None}
    users_by_email: dict[str, list[UserRow]] = {}
    for user in users:
        key = normalize_email(user.email)
        if key:
            users_by_email.setdefault(key, []).append(user)

    eligible = [p for p in people if profile_for_aggregator(p.aggregator_id)]
    emails_to_people: dict[str, list[PersonRow]] = {}
    for person in eligible:
        key = normalize_email(person.email)
        if key:
            emails_to_people.setdefault(key, []).append(person)

    actions: list[Action] = []
    for person in eligible:
        profile = profile_for_aggregator(person.aggregator_id)
        email = normalize_email(person.email)
        if email is None:
            actions.append(Action(SKIPPED, person.pessoa_id, person.nome, None, profile, "no_email"))
            continue

        if len(emails_to_people.get(email, [])) > 1:
            actions.append(Action(SKIPPED, person.pessoa_id, person.nome, email, profile, "email_collision"))
            continue

        linked = users_by_person.get(person.pessoa_id)
        if linked is not None:
            actions.append(Action(
                ALREADY_DONE, person.pessoa_id, person.nome, email, profile, "already_linked", linked.id
            ))
            continue

        matches = users_by_email.get(email, [])
        if len(matches) > 1:
            actions.append(Action(SKIPPED, person.pessoa_id, person.nome, email, profile, "email_collision"))
            continue
        if len(matches) == 1:
            existing = matches[0]
            if existing.person_id is not None and existing.person_id != person.pessoa_id:
                actions.append(Action(
                    SKIPPED, person.pessoa_id, person.nome, email, profile, "person_collision", existing.id
                ))
                continue
            if existing.person_id == person.pessoa_id:
                actions.append(Action(
                    ALREADY_DONE, person.pessoa_id, person.nome, email, profile, "already_linked", existing.id
                ))
                continue
            actions.append(Action(LINKED, person.pessoa_id, person.nome, email, profile, "link_existing", existing.id))
            continue

        actions.append(Action(CREATED, person.pessoa_id, person.nome, email, profile, "create_inactive"))

    return actions
