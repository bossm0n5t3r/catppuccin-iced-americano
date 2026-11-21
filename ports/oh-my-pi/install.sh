#!/usr/bin/env bash
# Install the generated theme using Bash, standard system utilities and omp.
set -euo pipefail

fail() {
  printf 'Error: %s\n' "$*" >&2
  exit 2
}

# Match a literal leading tilde; quoting it is intentional.
# shellcheck disable=SC2088
expand_home() {
  case "$1" in
  '~') printf '%s\n' "$HOME" ;;
  '~/'*) printf '%s/%s\n' "$HOME" "${1:2}" ;;
  *) printf '%s\n' "$1" ;;
  esac
}

# Resolve symlinks and dot segments even if the final file does not exist yet.
# Unlike realpath -m, this also works with the tools shipped with macOS.
resolve_path() {
  local remaining=$1 current=/ part link links=0
  [[ $remaining == /* ]] || remaining="$PWD/$remaining"
  while [[ -n $remaining ]]; do
    part=${remaining%%/*}
    if [[ $remaining == */* ]]; then remaining=${remaining#*/}; else remaining=; fi
    case "$part" in
    '' | .) continue ;;
    ..)
      current=${current%/*}
      current=${current:-/}
      continue
      ;;
    esac
    current="${current%/}/$part"
    if [[ -L $current ]]; then
      links=$((links + 1))
      [[ $links -le 40 ]] || fail "Too many symbolic links: $1"
      link=$(readlink "$current")
      if [[ $link == /* ]]; then current=/; else
        current=${current%/*}
        current=${current:-/}
      fi
      remaining="$link${remaining:+/$remaining}"
    fi
  done
  printf '%s\n' "$current"
}

agent_directory=
dark=false
while [[ $# -gt 0 ]]; do
  case "$1" in
  --agent-dir)
    [[ $# -ge 2 && $2 != --* ]] || fail '--agent-dir requires a path'
    agent_directory=$2
    shift 2
    [[ -n $agent_directory ]] || fail '--agent-dir requires a non-empty path'
    ;;
  --agent-dir=*)
    agent_directory=${1#*=}
    shift
    [[ -n $agent_directory ]] || fail '--agent-dir requires a non-empty path'
    ;;
  --dark)
    dark=true
    shift
    ;;
  -h | --help)
    printf 'Usage: %s [--agent-dir PATH] [--dark]\n' "$0"
    exit 0
    ;;
  *) fail "Unknown option: $1" ;;
  esac
done

command -v omp >/dev/null 2>&1 || fail 'omp not found on PATH; install oh-my-pi first'
script_directory=$(CDPATH='' cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
source="$script_directory/iced-americano.json"
[[ -f $source ]] || fail "Missing generated theme: $source"
agent_directory=$(expand_home "${agent_directory:-${PI_CODING_AGENT_DIR:-$HOME/.omp/agent}}")
target="$agent_directory/themes/iced-americano.json"

[[ ! -d $target || -L $target ]] || fail "Theme destination is a directory: $target"
if [[ ! -L $target && $(resolve_path "$target") == "$(resolve_path "$source")" ]]; then
  fail 'Theme destination must not be the source file'
fi

mkdir -p -- "$(dirname -- "$target")"
if [[ -L $target || -e $target ]]; then rm -- "$target"; fi
ln -s -- "$source" "$target"
printf 'Theme: %s -> %s\n' "$target" "$source"

# omp reads and writes config.yml in this agent directory.
export PI_CODING_AGENT_DIR=$agent_directory
keys=(theme.light)
if [[ $dark == true ]]; then keys+=(theme.dark); fi
for key in "${keys[@]}"; do
  omp config set "$key" iced-americano >/dev/null
  printf 'Config: %s = iced-americano\n' "$key"
done
