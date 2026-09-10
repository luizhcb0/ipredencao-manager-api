import unittest

from classify import (
    ALREADY_DONE,
    CREATED,
    LINKED,
    SKIPPED,
    Action,
    PersonRow,
    UserRow,
    classify,
    normalize_email,
    profile_for_aggregator,
)


class ClassifyTest(unittest.TestCase):
    def test_normalize_email(self):
        self.assertEqual(normalize_email("  Foo@Bar.COM "), "foo@bar.com")
        self.assertIsNone(normalize_email("   "))
        self.assertIsNone(normalize_email(None))

    def test_profile_mapping(self):
        self.assertEqual(profile_for_aggregator(2), "MEMBER")
        self.assertEqual(profile_for_aggregator(3), "MEMBER")
        self.assertEqual(profile_for_aggregator(5), "MEMBERSHIP_CANDIDATE")
        self.assertIsNone(profile_for_aggregator(1))
        self.assertIsNone(profile_for_aggregator(4))
        self.assertIsNone(profile_for_aggregator(10))

    def test_create_for_unique_email(self):
        people = [PersonRow(1, "Ana", "ana@igreja.com", 2)]
        actions = classify(people, [])
        self.assertEqual(actions, [
            Action(CREATED, 1, "Ana", "ana@igreja.com", "MEMBER", "create_inactive"),
        ])

    def test_link_existing_user_without_person(self):
        people = [PersonRow(1, "Ana", "ANA@igreja.com", 3)]
        users = [UserRow(9, "ana@igreja.com", None, "BOLETIM", True, "uid-9")]
        actions = classify(people, users)
        self.assertEqual(actions[0].outcome, LINKED)
        self.assertEqual(actions[0].usuario_id, 9)
        self.assertEqual(actions[0].profile, "MEMBER")

    def test_already_linked(self):
        people = [PersonRow(1, "Ana", "ana@igreja.com", 2)]
        users = [UserRow(9, "ana@igreja.com", 1, "BOLETIM", True, "uid-9")]
        self.assertEqual(classify(people, users)[0].outcome, ALREADY_DONE)

    def test_email_collision_among_people(self):
        people = [
            PersonRow(1, "Ana", "mesmo@igreja.com", 2),
            PersonRow(2, "Bia", "MESMO@igreja.com", 5),
        ]
        actions = classify(people, [])
        self.assertTrue(all(a.outcome == SKIPPED and a.reason == "email_collision" for a in actions))

    def test_person_collision_user_already_points_elsewhere(self):
        people = [PersonRow(1, "Ana", "ana@igreja.com", 2)]
        users = [UserRow(9, "ana@igreja.com", 99, "ADMIN", True, "uid-9")]
        action = classify(people, users)[0]
        self.assertEqual(action.outcome, SKIPPED)
        self.assertEqual(action.reason, "person_collision")

    def test_skips_ineligible_aggregator(self):
        people = [PersonRow(1, "Pastor", "pastor@igreja.com", 1)]
        self.assertEqual(classify(people, []), [])

    def test_skips_blank_email(self):
        people = [PersonRow(1, "Ana", "  ", 2)]
        action = classify(people, [])[0]
        self.assertEqual(action.outcome, SKIPPED)
        self.assertEqual(action.reason, "no_email")

    def test_candidate_profile(self):
        people = [PersonRow(1, "João", "joao@igreja.com", 5)]
        self.assertEqual(classify(people, [])[0].profile, "MEMBERSHIP_CANDIDATE")


if __name__ == "__main__":
    unittest.main()
