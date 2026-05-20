CREATE INDEX idx_matches_creator ON matches(creator_id);
CREATE INDEX idx_match_players_match ON match_players(match_id);
CREATE INDEX idx_match_players_user ON match_players(user_id);
CREATE INDEX idx_invites_code ON invites(code);
CREATE INDEX idx_invites_match ON invites(match_id);
