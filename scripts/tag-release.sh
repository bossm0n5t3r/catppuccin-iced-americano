#!/usr/bin/env bash
# Create an annotated v<version> tag from the release version and push it to trigger the
# release workflow. An optional HH:mm:ss sets the tag time (today, local time zone).
set -euo pipefail

fail() {
  printf 'Error: %s\n' "$*" >&2
  exit 2
}

usage() {
  printf 'Usage: %s [HH:mm:ss] [--no-push] [--remote NAME]\n' "$0"
}

time='' push=true remote=origin
while [[ $# -gt 0 ]]; do
  case "$1" in
  --no-push)
    push=false
    shift
    ;;
  --remote)
    [[ $# -ge 2 && -n $2 && $2 != --* ]] || fail '--remote requires a name'
    remote=$2
    shift 2
    ;;
  -h | --help)
    usage
    exit 0
    ;;
  --*) fail "Unknown option: $1" ;;
  *)
    [[ -z $time ]] || fail "Unexpected argument: $1"
    [[ $1 =~ ^([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$ ]] || fail "Time must be HH:mm:ss (00:00:00-23:59:59): $1"
    time=$1
    shift
    ;;
  esac
done

cd -- "$(git -C "$(dirname -- "${BASH_SOURCE[0]}")" rev-parse --show-toplevel)"

# The release version names the plugin ZIP and the Ghostty theme header. The palette
# version is versioned separately and only changes with the palette itself.
release_version=$(sed -n 's/^version=\(.*\)$/\1/p' gradle.properties)
[[ -n $release_version ]] || fail 'Could not read version from gradle.properties'
tag="v$release_version"

[[ -z $(git status --porcelain) ]] || fail 'Working tree has uncommitted changes'
branch=$(git symbolic-ref --quiet --short HEAD) || fail 'HEAD is detached'
[[ $branch == master ]] || fail "Not on master: $branch"
! git rev-parse --quiet --verify "refs/tags/$tag" >/dev/null || fail "Tag already exists locally: $tag"

if [[ $push == true ]]; then
  git fetch --quiet "$remote" master
  [[ -z $(git ls-remote --tags "$remote" "refs/tags/$tag") ]] || fail "Tag already exists on $remote: $tag"
  # Release links on the site point at master; tag exactly what is published there.
  [[ $(git rev-parse HEAD) == "$(git rev-parse "$remote/master")" ]] ||
    fail "HEAD does not match $remote/master; push or pull master first"
fi

if [[ -n $time ]]; then
  # Git reads "YYYY-MM-DD HH:mm:ss" in the local time zone.
  GIT_COMMITTER_DATE="$(date +%Y-%m-%d) $time" git tag -a "$tag" -m "$tag"
else
  git tag -a "$tag" -m "$tag"
fi
printf 'Created %s at %s\n' "$tag" "$(git for-each-ref --format='%(taggerdate:iso)' "refs/tags/$tag")"

if [[ $push == true ]]; then
  # Drop the local tag on failure so the script can simply be re-run.
  git push "$remote" "refs/tags/$tag" || {
    git tag -d "$tag" >/dev/null
    fail "Push failed; removed local tag $tag"
  }
  printf 'Pushed %s to %s\n' "$tag" "$remote"
else
  printf 'Push with: git push %s %s\n' "$remote" "$tag"
fi
